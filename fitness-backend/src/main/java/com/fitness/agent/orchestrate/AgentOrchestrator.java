package com.fitness.agent.orchestrate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.agent.dto.AgentResponse;
import com.fitness.agent.dto.AiCourseAction;
import com.fitness.agent.dto.AiCourseRequest;
import com.fitness.agent.dto.AiCourseResult;
import com.fitness.agent.dto.AiFeedbackRequest;
import com.fitness.agent.dto.AiPlanCourse;
import com.fitness.agent.dto.IntentResult;
import com.fitness.agent.knowledge.KnowledgeSearch;
import com.fitness.agent.llm.DeepSeekClient;
import com.fitness.agent.rule.CourseAssembler;
import com.fitness.agent.rule.PlanAssembler;
import com.fitness.agent.safety.SafetyGuard;
import com.fitness.agent.session.AiSessionService;
import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.entity.Course;
import com.fitness.entity.CourseAction;
import com.fitness.entity.KnowledgeQa;
import com.fitness.entity.PlanCourse;
import com.fitness.entity.TrainingPlan;
import com.fitness.entity.TrainingRecord;
import com.fitness.entity.User;
import com.fitness.mapper.CourseActionMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.PlanCourseMapper;
import com.fitness.mapper.TrainingPlanMapper;
import com.fitness.mapper.TrainingRecordMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 编排器：安全检测 → 意图识别 → 工具编排 → 规则组装 → 生成说明 → 返回
 * <p>LLM 不可用时走规则兜底（意图关键词匹配 + 模板文案），保证功能可用。
 */
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final DeepSeekClient deepSeekClient;
    private final CourseAssembler courseAssembler;
    private final PlanAssembler planAssembler;
    private final SafetyGuard safetyGuard;
    private final KnowledgeSearch knowledgeSearch;
    private final UserMapper userMapper;
    private final AiSessionService sessionService;
    private final CourseMapper courseMapper;
    private final CourseActionMapper courseActionMapper;
    private final TrainingPlanMapper planMapper;
    private final PlanCourseMapper planCourseMapper;
    private final TrainingRecordMapper trainingRecordMapper;

    @Transactional
    public AgentResponse handle(Long userId, String input) {
        // 1. 安全兜底（双层拦截：关键词 + LLM 分类）
        String block = safetyGuard.check(input);
        if (block == null && deepSeekClient.isConfigured()) {
            if (deepSeekClient.isUnsafe(input)) {
                block = "该话题涉及健康与安全，建议咨询专业医生或教练，我无法提供相关建议。";
            }
        }
        if (block != null) {
            return AgentResponse.of("text", block, null);
        }

        // 2. 意图识别（LLM，失败降级规则）
        IntentResult intent = recognize(input);

        // 3. 按意图分发
        String it = intent.getIntent() == null ? "chat" : intent.getIntent();
        return switch (it) {
            case "recommend_course" -> recommendCourse(userId, intent, input);
            case "ask_question" -> answerQuestion(userId, intent, input);
            case "create_plan" -> createPlan(userId, intent, input);
            default -> chat(userId, input);
        };
    }

    /** 直接课程生成（REST /api/ai/course，跳过意图识别），自动补全用户画像与禁忌并落库 */
    @Transactional
    public AiCourseResult course(Long userId, AiCourseRequest req) {        User user = userMapper.selectById(userId);
        if (req.getGoal() == null) req.setGoal(user != null ? user.getGoal() : null);
        int goal = req.getGoal() == null ? 1 : req.getGoal();
        int level = req.getLevel() == null ? (user != null && user.getLevel() != null ? user.getLevel() : 1) : req.getLevel();
        req.setLevel(adjustByHistory(userId, level));
        req.setContraindications(mergeContraOf(req.getContraindications(), user));
        AiCourseResult course = courseAssembler.assemble(req);
        course.setCourseId(saveCourse(userId, course, goal, req.getLevel()));
        return course;
    }

    private IntentResult recognize(String input) {
        if (deepSeekClient.isConfigured()) {
            IntentResult r = deepSeekClient.parseIntent(input);
            if (r != null && r.getIntent() != null && !r.getIntent().isBlank()) {
                return r;
            }
        }
        return ruleIntent(input);
    }

    /** 规则兜底意图识别（LLM 不可用或输出非法时），含简单槽位抽取 */
    private IntentResult ruleIntent(String input) {
        IntentResult r = new IntentResult();
        if (containsAny(input, "计划", "周", "制定", "安排", "阶段")) {
            r.setIntent("create_plan");
        } else if (containsAny(input, "怎么", "什么", "为什么", "如何", "多少", "区别", "是否", "能不能", "可以吗")) {
            r.setIntent("ask_question");
            r.setQuestion(input);
        } else if (containsAny(input, "练", "训练", "课程", "动作", "健身", "胸", "背", "腿", "肩", "手臂", "核心", "全身",
                "哑铃", "杠铃", "绳索", "弹力带", "壶铃", "减脂", "增肌", "塑形")) {
            r.setIntent("recommend_course");
            extractSlots(r, input);
        } else {
            r.setIntent("chat");
        }
        return r;
    }

    /** 从话术抽取简单槽位（部位/器械/时长/目标/禁忌），供规则兜底使用 */
    private void extractSlots(IntentResult r, String input) {
        List<String> muscles = new ArrayList<>();
        for (String p : List.of("胸", "背", "肩", "手臂", "腿", "核心", "全身")) {
            if (input.contains(p)) muscles.add(p);
        }
        if (!muscles.isEmpty()) r.setMuscles(muscles);

        List<String> equipment = new ArrayList<>();
        for (String e : List.of("哑铃", "杠铃", "绳索", "弹力带", "壶铃", "史密斯机", "固定器械")) {
            if (input.contains(e)) equipment.add(e);
        }
        if (!equipment.isEmpty()) r.setEquipment(equipment);

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,3})\\s*分钟").matcher(input);
        if (m.find()) r.setDuration(Integer.parseInt(m.group(1)));

        if (input.contains("增肌")) r.setGoal(1);
        else if (input.contains("减脂")) r.setGoal(2);
        else if (input.contains("塑形")) r.setGoal(3);

        List<String> contra = new ArrayList<>();
        for (String c : List.of("腰伤", "膝伤", "肩伤")) {
            if (input.contains(c)) contra.add(c);
        }
        if (!contra.isEmpty()) r.setContraindications(contra);
    }

    private AgentResponse recommendCourse(Long userId, IntentResult intent, String input) {
        User user = userMapper.selectById(userId);

        int goal = intent.getGoal() != null ? intent.getGoal() : (user != null && user.getGoal() != null ? user.getGoal() : 1);
        int level = intent.getLevel() != null ? intent.getLevel() : (user != null && user.getLevel() != null ? user.getLevel() : 1);
        // 反馈闭环：近 30 天训练频繁则上调难度（渐进超负荷），冷启动保持默认
        level = adjustByHistory(userId, level);

        AiCourseRequest req = new AiCourseRequest();
        req.setGoal(goal);
        req.setLevel(level);
        req.setMuscles(intent.getMuscles());
        req.setEquipment(intent.getEquipment());
        req.setDuration(intent.getDuration());
        // 禁忌 = 意图槽位 + 用户画像（合并）
        req.setContraindications(mergeContra(intent, user));

        AiCourseResult course = courseAssembler.assemble(req);
        // 课程落库（仅本人可见）
        course.setCourseId(saveCourse(userId, course, goal, level));

        String desc = deepSeekClient.isConfigured() ? deepSeekClient.describeCourse(course) : null;
        if (desc == null || desc.isBlank()) {
            desc = buildCourseDesc(course);
        }

        sessionService.append(userId, "user", input);
        sessionService.append(userId, "assistant", desc);

        Map<String, Object> data = new HashMap<>();
        data.put("courseId", course.getCourseId());
        data.put("course", course);
        return AgentResponse.of("course", desc, data);
    }

    private AgentResponse answerQuestion(Long userId, IntentResult intent, String input) {
        String q = intent.getQuestion() != null ? intent.getQuestion() : input;
        KnowledgeQa qa = knowledgeSearch.search(q);
        String answer;
        if (qa != null) {
            answer = qa.getAnswer();
            if (qa.getSource() != null && !qa.getSource().isBlank()) {
                answer += "\n（来源：" + qa.getSource() + "）";
            }
        } else if (deepSeekClient.isConfigured()) {
            answer = deepSeekClient.answerQuestion(q);
            if (answer == null || answer.isBlank()) {
                answer = "抱歉，这个问题我暂时无法回答，建议咨询专业教练。";
            }
        } else {
            answer = "抱歉，这个问题我暂时无法回答，建议咨询专业教练。";
        }

        sessionService.append(userId, "user", input);
        sessionService.append(userId, "assistant", answer);
        return AgentResponse.of("text", answer, null);
    }

    /** 学习计划生成（F7）：规则引擎生成 N 周计划并落库 training_plan + plan_course */
    private AgentResponse createPlan(Long userId, IntentResult intent, String input) {
        User user = userMapper.selectById(userId);
        int goal = intent.getGoal() != null ? intent.getGoal() : (user != null && user.getGoal() != null ? user.getGoal() : 1);
        int level = intent.getLevel() != null ? intent.getLevel() : (user != null && user.getLevel() != null ? user.getLevel() : 1);
        List<String> contra = mergeContra(intent, user);

        int weeks = intent.getWeeks() != null ? intent.getWeeks() : PlanAssembler.DEFAULT_WEEKS;
        Map<String, Object> data = savePlan(userId, goal, level, intent.getEquipment(), intent.getMuscles(), contra, intent.getDuration(), weeks);
        int totalDays = ((List<?>) data.get("days")).size();

        // LLM 周期化说明（失败用模板文案兜底）
        String desc = deepSeekClient.isConfigured()
                ? deepSeekClient.describePlan(planStruct(goal, level, weeks, totalDays))
                : null;
        if (desc == null || desc.isBlank()) {
            desc = buildPlanDesc(goal, level, weeks, totalDays);
        }

        sessionService.append(userId, "user", input);
        sessionService.append(userId, "assistant", desc);
        return AgentResponse.of("plan", desc, data);
    }

    /** 直接生成学习计划（REST /api/ai/plan），自动补全用户画像 */
    @Transactional
    public Map<String, Object> plan(Long userId, AiCourseRequest req) {
        User user = userMapper.selectById(userId);
        if (req.getGoal() == null) req.setGoal(user != null ? user.getGoal() : null);
        int goal = req.getGoal() == null ? 1 : req.getGoal();
        int level = req.getLevel() == null ? (user != null && user.getLevel() != null ? user.getLevel() : 1) : req.getLevel();
        List<String> contra = mergeContraOf(req.getContraindications(), user);
        int weeks = req.getWeeks() != null ? req.getWeeks() : PlanAssembler.DEFAULT_WEEKS;
        return savePlan(userId, goal, level, req.getEquipment(), req.getMuscles(), contra, req.getDuration(), weeks);
    }

    /** 训练反馈（F5）：自评难度回灌用户画像（太简单→level+1，太难→level-1） */
    @Transactional
    public Map<String, Object> feedback(Long userId, AiFeedbackRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        int level = user.getLevel() == null ? 1 : user.getLevel();
        int difficulty = req.getDifficulty() == null ? 2 : req.getDifficulty();
        Integer completion = req.getCompletion();

        // 查训练记录：落库自评难度 + 推导完成度
        TrainingRecord rec = null;
        if (req.getRecordId() != null) {
            rec = trainingRecordMapper.selectById(req.getRecordId());
            if (rec != null && userId.equals(rec.getUserId())) {
                rec.setFeedback(difficulty);
                trainingRecordMapper.updateById(rec);
            } else {
                rec = null; // 非本人记录，忽略
            }
        }

        // 完成度回灌：显式 completion < 60，或训练记录完成动作 < 4（课程通常 5-6 动作）
        boolean lowCompletion = (completion != null && completion < 60)
                || (rec != null && rec.getCompletedCount() != null && rec.getCompletedCount() < 4);

        String message;
        if (lowCompletion && level > 1) {
            level -= 1;
            message = "本次完成度偏低，已为您下调训练难度";
        } else if (difficulty == 1 && level < 3) {
            level += 1;
            message = "已为您上调训练难度";
        } else if (difficulty == 3 && level > 1) {
            level -= 1;
            message = "已为您下调训练难度";
        } else {
            message = "难度适中，保持不变";
        }
        user.setLevel(level);
        userMapper.updateById(user);
        Map<String, Object> data = new HashMap<>();
        data.put("level", level);
        data.put("message", message);
        return data;
    }

    /** 学习计划落库（training_plan + 每日课程 + plan_course），返回计划数据 */
    private Map<String, Object> savePlan(Long userId, int goal, int level, List<String> equipment,
                                         List<String> muscles, List<String> contra, Integer duration, Integer weeks) {
        int wks = weeks == null ? PlanAssembler.DEFAULT_WEEKS : weeks;
        List<AiPlanCourse> planCourses = planAssembler.assemble(goal, level, equipment, muscles, contra, duration, wks);

        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setName(planName(goal, wks));
        plan.setGoal(goal);
        plan.setLevel(level);
        plan.setCycle(cycleOf(level));
        plan.setTags("AI私教," + wks + "周");
        plan.setIsTemplate(0);
        planMapper.insert(plan);

        List<Map<String, Object>> days = new ArrayList<>();
        for (AiPlanCourse pc : planCourses) {
            Long courseId = saveCourse(userId, pc.getCourse(), goal, level);
            PlanCourse rel = new PlanCourse();
            rel.setPlanId(plan.getId());
            rel.setCourseId(courseId);
            rel.setDayNo(pc.getDayNo());
            rel.setWeek(pc.getWeek());
            rel.setSort(pc.getDayNo());
            planCourseMapper.insert(rel);

            Map<String, Object> d = new HashMap<>();
            d.put("dayNo", pc.getDayNo());
            d.put("week", pc.getWeek());
            d.put("courseId", courseId);
            d.put("courseName", pc.getCourse().getName());
            days.add(d);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("planId", plan.getId());
        data.put("name", plan.getName());
        data.put("goal", goal);
        data.put("level", level);
        data.put("days", days);
        return data;
    }

    private AgentResponse chat(Long userId, String input) {
        String reply = null;
        if (deepSeekClient.isConfigured()) {
            reply = deepSeekClient.chatReply(input);
        }
        if (reply == null || reply.isBlank()) {
            reply = "我是你的 AI 私教，可以帮你生成训练课程、解答健身知识。试试说「我只有哑铃，想练胸 30 分钟」~";
        }
        sessionService.append(userId, "user", input);
        sessionService.append(userId, "assistant", reply);
        return AgentResponse.of("text", reply, null);
    }

    /** 强制知识问答（REST /api/ai/qa），不做意图识别 */
    public AgentResponse answer(Long userId, String question) {
        String block = safetyGuard.check(question);
        if (block == null && deepSeekClient.isConfigured() && deepSeekClient.isUnsafe(question)) {
            block = "该话题涉及健康与安全，建议咨询专业医生或教练，我无法提供相关建议。";
        }
        if (block != null) {
            return AgentResponse.of("text", block, null);
        }
        IntentResult intent = new IntentResult();
        intent.setIntent("ask_question");
        intent.setQuestion(question);
        return answerQuestion(userId, intent, question);
    }

    private List<String> mergeContra(IntentResult intent, User user) {
        return mergeContraOf(intent.getContraindications(), user);
    }

    private List<String> mergeContraOf(List<String> fromSlot, User user) {
        List<String> contra = new ArrayList<>();
        if (fromSlot != null) {
            contra.addAll(fromSlot);
        }
        if (user != null && user.getContraindication() != null && !user.getContraindication().isBlank()) {
            for (String c : user.getContraindication().split(",")) {
                String t = c.trim();
                if (!t.isEmpty() && !contra.contains(t)) {
                    contra.add(t);
                }
            }
        }
        return contra;
    }

    private String buildCourseDesc(AiCourseResult course) {
        StringBuilder sb = new StringBuilder();
        sb.append("为你定制了「").append(course.getName()).append("」，预计 ").append(course.getDuration()).append(" 分钟：\n");
        for (AiCourseAction a : course.getActions()) {
            sb.append("· ").append(a.getName()).append("：").append(a.getSets()).append(" 组 × ").append(a.getReps()).append(" 次\n");
        }
        if (course.getNote() != null && !course.getNote().isBlank()) {
            sb.append("（").append(course.getNote()).append("）");
        }
        return sb.toString().trim();
    }

    private boolean containsAny(String text, String... kws) {
        for (String kw : kws) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /** 课程落库（course + course_action），status=4 仅本人可见，返回课程 ID */
    private Long saveCourse(Long userId, AiCourseResult result, int goal, int level) {
        Course c = new Course();
        c.setName(result.getName());
        c.setCategory(goalName(goal));
        c.setPart(result.getCoveredParts() != null && !result.getCoveredParts().isEmpty()
                ? result.getCoveredParts().get(0) : "全身");
        c.setIntro(result.getNote());
        c.setDifficulty(level);
        c.setDuration(result.getDuration());
        c.setCalorie(result.getCalorie());
        c.setUserId(userId);
        c.setStatus(4); // 仅本人可见
        c.setViewCount(0);
        c.setFavoriteCount(0);
        c.setLikeCount(0);
        courseMapper.insert(c);

        int sort = 1;
        if (result.getActions() != null) {
            for (AiCourseAction a : result.getActions()) {
                if (a.getActionId() == null) continue;
                CourseAction ca = new CourseAction();
                ca.setCourseId(c.getId());
                ca.setActionId(a.getActionId());
                ca.setSort(sort++);
                ca.setSets(a.getSets());
                ca.setReps(a.getReps());
                courseActionMapper.insert(ca);
            }
        }
        return c.getId();
    }

    /** 反馈闭环：近 30 天训练次数 ≥6 次则上调一级难度（渐进超负荷），否则保持 */
    private int adjustByHistory(Long userId, int level) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            Long cnt = trainingRecordMapper.selectCount(new LambdaQueryWrapper<TrainingRecord>()
                    .eq(TrainingRecord::getUserId, userId)
                    .ge(TrainingRecord::getStartTime, since));
            if (cnt != null && cnt >= 6) {
                return Math.min(3, level + 1);
            }
        } catch (Exception e) {
            // 忽略异常，保持默认难度
        }
        return level;
    }

    /** 学习计划周期化说明（模板文案） */
    private String buildPlanDesc(int goal, int level, int weeks, int totalDays) {
        int daysPerWeek = splitDaysPerWeek(level);
        return "为你制定了「" + planName(goal, weeks) + "」共 " + weeks + " 周计划（每周 " + daysPerWeek + " 练，共 " + totalDays + " 次课程）：\n"
                + "· 第 1 周：适应期（容量降低，建立动作模式）\n"
                + "· 中间周：递增期（组次逐步提升，渐进超负荷）\n"
                + "· 最后一周：减载期（主动恢复）\n"
                + "已加入你的「我的计划」，可随时查看。";
    }

    /** 计划结构摘要（供 LLM 周期化说明） */
    private String planStruct(int goal, int level, int weeks, int totalDays) {
        int daysPerWeek = splitDaysPerWeek(level);
        return planName(goal, weeks) + "，共 " + weeks + " 周，每周 " + daysPerWeek + " 练，共 " + totalDays + " 次课程；"
                + "周期化：第 1 周适应期（容量 0.7），中间每周 +5% 递增，最后一周减载（0.6）。";
    }

    private int splitDaysPerWeek(int level) {
        return level >= 2 ? 4 : 3;
    }

    private String planName(int goal, int weeks) {
        return weeks + "周" + goalName(goal) + "计划";
    }

    private String cycleOf(int level) {
        return "每周" + splitDaysPerWeek(level) + "练";
    }

    private String goalName(int goal) {
        return switch (goal) {
            case 2 -> "减脂";
            case 3 -> "塑形";
            case 4 -> "保持";
            default -> "增肌";
        };
    }
}
