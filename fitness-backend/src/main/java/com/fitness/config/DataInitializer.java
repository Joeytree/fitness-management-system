package com.fitness.config;

import com.fitness.entity.Action;
import com.fitness.entity.Admin;
import com.fitness.entity.Article;
import com.fitness.entity.Course;
import com.fitness.entity.Food;
import com.fitness.entity.Permission;
import com.fitness.entity.PlanAction;
import com.fitness.entity.Recipe;
import com.fitness.entity.Role;
import com.fitness.entity.RolePermission;
import com.fitness.entity.TrainingPlan;
import com.fitness.entity.User;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.AdminMapper;
import com.fitness.mapper.ArticleMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.FoodMapper;
import com.fitness.mapper.PermissionMapper;
import com.fitness.mapper.PlanActionMapper;
import com.fitness.mapper.RecipeMapper;
import com.fitness.mapper.RoleMapper;
import com.fitness.mapper.RolePermissionMapper;
import com.fitness.mapper.TrainingPlanMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 数据初始化器：首次启动时注入种子数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final ActionMapper actionMapper;
    private final CourseMapper courseMapper;
    private final FoodMapper foodMapper;
    private final RecipeMapper recipeMapper;
    private final TrainingPlanMapper planMapper;
    private final PlanActionMapper planActionMapper;
    private final ArticleMapper articleMapper;
    private final com.fitness.mapper.MomentMapper momentMapper;
    private final com.fitness.mapper.MomentCommentMapper momentCommentMapper;
    private final com.fitness.mapper.CourseCommentMapper courseCommentMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        initRoleAndPermission();
        initAdmin();
        initUser();
        initAction();
        initFood();
        initRecipe();
        initCourse();
        initPlanTemplate();
        initArticle();
        initReviewData();
        log.info("种子数据初始化完成");
    }

    private void initRoleAndPermission() {
        if (roleMapper.selectCount(null) > 0) {
            return;
        }
        Role superAdmin = insertRole("超级管理员", "SUPER_ADMIN", "拥有全部权限");
        Role editor = insertRole("内容编辑", "EDITOR", "负责课程/食物/资讯内容管理");

        // 菜单权限
        long sort = 1;
        Permission dashboard = insertPerm("数据仪表盘", "dashboard", 1, null, "/dashboard", sort++);
        Permission user = insertPerm("用户管理", "user", 1, null, "/user", sort++);
        Permission course = insertPerm("课程管理", "course", 1, null, "/course", sort++);
        Permission action = insertPerm("动作管理", "action", 1, null, "/action", sort++);
        Permission review = insertPerm("内容审核", "review", 1, null, "/review", sort++);
        Permission food = insertPerm("食物库管理", "food", 1, null, "/food", sort++);
        Permission recipe = insertPerm("食谱管理", "recipe", 1, null, "/recipe", sort++);
        Permission article = insertPerm("资讯管理", "article", 1, null, "/article", sort++);
        Permission system = insertPerm("系统管理", "system", 1, null, "/system", sort++);
        Permission role = insertPerm("角色管理", "system:role", 1, system.getId(), "/system/role", sort++);
        Permission perm = insertPerm("权限管理", "system:permission", 1, system.getId(), "/system/permission", sort++);
        Permission admin = insertPerm("管理员管理", "system:admin", 1, system.getId(), "/system/admin", sort++);

        // 超级管理员拥有全部权限
        Permission[] all = {dashboard, user, course, action, review, food, recipe, article, system, role, perm, admin};
        for (Permission p : all) {
            rolePermissionMapper.insert(buildRolePerm(superAdmin.getId(), p.getId()));
        }
        // 内容编辑拥有内容相关权限
        Permission[] editorPerms = {dashboard, course, action, review, food, recipe, article};
        for (Permission p : editorPerms) {
            rolePermissionMapper.insert(buildRolePerm(editor.getId(), p.getId()));
        }
    }

    private void initAdmin() {
        if (adminMapper.selectCount(null) > 0) {
            return;
        }
        Role superAdmin = roleMapper.selectList(null).stream()
                .filter(r -> "SUPER_ADMIN".equals(r.getCode())).findFirst().orElse(null);
        Role editor = roleMapper.selectList(null).stream()
                .filter(r -> "EDITOR".equals(r.getCode())).findFirst().orElse(null);
        if (superAdmin == null) {
            return;
        }
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setNickname("系统管理员");
        admin.setRoleId(superAdmin.getId());
        admin.setStatus(1);
        adminMapper.insert(admin);

        if (editor != null) {
            Admin ed = new Admin();
            ed.setUsername("editor");
            ed.setPassword(encoder.encode("admin123"));
            ed.setNickname("内容编辑");
            ed.setRoleId(editor.getId());
            ed.setStatus(1);
            adminMapper.insert(ed);
        }
    }

    private void initUser() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        insertUser("13800000001", "123456", "健身小白", 1, 175, 1, 1);
        insertUser("13800000002", "123456", "撸铁达人", 1, 180, 1, 3);
    }

    private void initAction() {
        if (actionMapper.selectCount(null) > 0) {
            return;
        }
        insertAction("杠铃卧推", "胸", 2, "杠铃", "仰卧平凳，双手略宽于肩握杠。下放至胸部轻触，再推起至手臂伸直。",
                "肩胛骨后收下沉，保持核心稳定", "手腕过度后折、臀部离凳", "下放吸气，推起呼气", 1, null);
        insertAction("俯卧撑", "胸", 1, "无", "双手撑地与肩同宽，身体成直线，屈肘下放，胸部接近地面后推起。",
                "核心收紧，身体不塌腰", "塌腰、肘部过度外展", "下放吸气，推起呼气", 3, null);
        insertAction("引体向上", "背", 2, "单杠", "双手略宽于肩握杠，背部发力将身体拉起，下巴过杠后缓慢下放。",
                "肩胛骨下沉，避免耸肩", "借助惯性摆动", "拉起呼气，下放吸气", 1, null);
        insertAction("杠铃划船", "背", 2, "杠铃", "屈髋俯身，背部平直，将杠铃拉向腹部，再缓慢下放。",
                "保持背部中立，避免弓背", "用腰发力、身体过度晃动", "拉起呼气，下放吸气", 3, null);
        insertAction("哑铃推举", "肩", 2, "哑铃", "坐姿，双手持哑铃于肩部，向上推举至手臂伸直，再缓慢下放。",
                "核心收紧，避免过度后仰", "腰椎过伸、借力推举", "推起呼气，下放吸气", 3, null);
        insertAction("哑铃侧平举", "肩", 1, "哑铃", "站立，双手持哑铃于体侧，向两侧平举至与肩同高，再缓慢下放。",
                "肘部微屈，控制下放速度", "耸肩、摆动借力", "平举呼气，下放吸气", 1, null);
        insertAction("哑铃弯举", "手臂", 1, "哑铃", "站立，双手持哑铃，前臂弯举至顶峰收缩，再缓慢下放。",
                "大臂贴近身体保持固定", "身体后仰借力", "弯举呼气，下放吸气", 3, null);
        insertAction("绳索下压", "手臂", 1, "龙门架", "站姿，双手握绳，大臂固定，前臂下压至伸直，再缓慢还原。",
                "大臂夹紧身体", "大臂前移借力", "下压呼气，还原吸气", 1, null);
        insertAction("杠铃深蹲", "腿", 3, "杠铃", "杠铃置于斜方肌，双脚与肩同宽，屈膝屈髋下蹲至大腿平行地面，再站起。",
                "膝盖与脚尖方向一致，背部中立", "膝盖内扣、弓背", "下蹲吸气，站起呼气", 1, null);
        insertAction("硬拉", "腿", 3, "杠铃", "双脚与肩同宽，屈髋俯身握杠，背部平直，伸髋伸膝将杠铃拉起至直立。",
                "杠铃贴近身体，背部始终平直", "弓背发力", "拉起呼气，下放吸气", 3, null);
        insertAction("臀桥", "臀", 1, "无", "仰卧屈膝，双脚踩地，臀部发力向上顶起至身体成直线，再缓慢下放。",
                "顶端收紧臀部", "腰部代偿过度后伸", "顶起呼气，下放吸气", 3, null);
        insertAction("箭步蹲", "臀", 2, "哑铃", "双手持哑铃，向前跨步下蹲，前腿大小腿约90度，再蹬地还原。",
                "前膝不超过脚尖", "膝盖内扣、步幅过小", "下蹲吸气，站起呼气", 3, null);
        insertAction("平板支撑", "核心", 1, "无", "肘撑地，身体成直线，核心收紧保持静止。",
                "臀部不过度抬高或下沉", "塌腰、抬头", "均匀呼吸", 3, null);
        insertAction("卷腹", "核心", 1, "无", "仰卧屈膝，用腹部力量将上背部卷离地面，再缓慢还原。",
                "下背贴地，用腹发力", "用手抱头拉扯颈部", "卷起呼气，还原吸气", 3, null);
        insertAction("波比跳", "全身", 3, "无", "下蹲撑地，后跳成平板，做俯卧撑后收腿，向上跳起。",
                "动作连贯，核心收紧", "塌腰、落地过重", "跳跃时呼气", 1, null);
        insertAction("开合跳", "全身", 1, "无", "站立，跳起时双脚分开双臂上举，再跳回并拢。",
                "落地缓冲，保持节奏", "落地僵硬、膝盖过直", "均匀呼吸", 1, null);
    }

    private void initFood() {
        if (foodMapper.selectCount(null) > 0) {
            return;
        }
        insertFood("米饭", "主食", "116", "2.6", "25.9", "0.3", "g");
        insertFood("全麦面包", "主食", "246", "9.0", "46.0", "3.5", "g");
        insertFood("燕麦", "主食", "367", "15.0", "61.0", "6.7", "g");
        insertFood("鸡胸肉", "蛋白", "118", "24.0", "0.6", "1.2", "g");
        insertFood("鸡蛋", "蛋白", "144", "13.3", "2.8", "8.8", "个");
        insertFood("牛肉", "蛋白", "125", "20.2", "1.2", "4.2", "g");
        insertFood("三文鱼", "蛋白", "139", "17.2", "0.0", "7.8", "g");
        insertFood("西兰花", "蔬果", "34", "4.1", "4.3", "0.6", "g");
        insertFood("苹果", "蔬果", "53", "0.4", "13.7", "0.2", "个");
        insertFood("香蕉", "蔬果", "93", "1.4", "22.0", "0.2", "根");
        insertFood("牛奶", "饮品", "54", "3.0", "3.4", "3.2", "ml");
        insertFood("酸奶", "饮品", "72", "2.5", "9.3", "2.7", "ml");
        insertFood("杏仁", "零食", "578", "21.3", "21.7", "50.0", "g");
        insertFood("红薯", "主食", "90", "1.4", "20.1", "0.2", "g");
        insertFood("虾仁", "蛋白", "93", "18.6", "2.8", "0.8", "g");
        insertFood("豆腐", "蛋白", "81", "8.1", "3.8", "3.7", "g");
    }

    private void initRecipe() {
        if (recipeMapper.selectCount(null) > 0) {
            return;
        }
        insertRecipe("增肌鸡胸藜麦碗", 1, "增肌", "鸡胸肉煎熟切块，藜麦煮熟，与西兰花、牛油果拌匀，撒黑胡椒。",
                520, "45.0", "50.0", "14.0", null);
        insertRecipe("高蛋白牛肉意面", 1, "增肌", "牛肉切丝炒熟，加入全麦意面与番茄酱翻炒，最后加菠菜。",
                610, "40.0", "70.0", "18.0", null);
        insertRecipe("减脂鸡胸沙拉", 2, "减脂", "鸡胸肉水煮撕丝，搭配生菜、黄瓜、小番茄，淋油醋汁。",
                320, "35.0", "15.0", "10.0", null);
        insertRecipe("低卡蒸鱼蔬菜", 2, "减脂", "鲈鱼清蒸，配西兰花与胡萝卜，少油少盐调味。",
                280, "32.0", "12.0", "8.0", null);
    }

    private void initCourse() {
        if (courseMapper.selectCount(null) > 0) {
            return;
        }
        insertCourse("胸部增肌入门", "增肌", "针对胸部的入门训练，含卧推与俯卧撑讲解", 1, 40, 250);
        insertCourse("背部力量进阶", "增肌", "强化背部肌群，引体向上与划船系统训练", 2, 50, 320);
        insertCourse("全身燃脂 HIIT", "HIIT", "高强度间歇训练，快速燃脂提升心肺", 3, 30, 400);
        insertCourse("新手塑形基础", "塑形", "适合新手的全身塑形训练", 1, 35, 200);
    }

    private void initPlanTemplate() {
        if (planMapper.selectCount(null) > 0) {
            return;
        }
        // 模板1：增肌新手（每周3练，分3天）
        TrainingPlan p1 = insertPlan(null, "增肌新手入门计划", 1, 1, "每周3练", "增肌,新手", 1);
        bindAction(p1.getId(), 1L, 1, 3, 12, 60, 1);   // Day1 杠铃卧推
        bindAction(p1.getId(), 2L, 1, 3, 15, 45, 2);   // Day1 俯卧撑
        bindAction(p1.getId(), 9L, 2, 3, 12, 90, 3);   // Day2 深蹲
        bindAction(p1.getId(), 15L, 3, 3, 10, 60, 4);  // Day3 波比跳
        // 模板2：增肌进阶（每周4练，分3天）
        TrainingPlan p2 = insertPlan(null, "增肌进阶分化计划", 1, 3, "每周4练", "增肌,进阶", 1);
        bindAction(p2.getId(), 3L, 1, 4, 10, 90, 1);   // Day1 引体向上
        bindAction(p2.getId(), 4L, 2, 4, 12, 90, 2);   // Day2 杠铃划船
        bindAction(p2.getId(), 10L, 3, 4, 10, 120, 3); // Day3 硬拉
        // 模板3：减脂新手（每周4练，分3天）
        TrainingPlan p3 = insertPlan(null, "减脂燃脂计划", 2, 1, "每周4练", "减脂,新手", 1);
        bindAction(p3.getId(), 15L, 1, 4, 12, 45, 1);  // Day1 波比跳
        bindAction(p3.getId(), 16L, 2, 4, 20, 30, 2);  // Day2 开合跳
        bindAction(p3.getId(), 13L, 3, 3, 60, 45, 3);  // Day3 平板支撑
        // 模板4：塑形进阶（每周5练，分3天）
        TrainingPlan p4 = insertPlan(null, "塑形全身计划", 3, 3, "每周5练", "塑形,进阶", 1);
        bindAction(p4.getId(), 5L, 1, 4, 12, 60, 1);   // Day1 哑铃推举
        bindAction(p4.getId(), 12L, 2, 4, 12, 60, 2);  // Day2 箭步蹲
        bindAction(p4.getId(), 14L, 3, 4, 15, 45, 3);  // Day3 卷腹
    }

    private void initArticle() {
        if (articleMapper.selectCount(null) > 0) {
            return;
        }
        insertArticle("增肌期饮食原则", 1, "增肌期应保证热量盈余，蛋白质摄入建议每公斤体重1.6-2.2g，碳水与脂肪合理搭配，训练前后补充快碳与蛋白质。", null);
        insertArticle("减脂期热量缺口怎么算", 1, "减脂核心是制造热量缺口，建议每日缺口300-500千卡，同时保证蛋白质摄入，避免肌肉流失。", null);
        insertArticle("系统上线公告", 3, "智慧健身系统正式上线，欢迎使用课程学习、饮食记录、健康监测等功能。", null);
    }

    private void initReviewData() {
        if (momentMapper.selectCount(null) > 0) {
            return;
        }
        // 待审核动态（status=0）
        com.fitness.entity.Moment m1 = new com.fitness.entity.Moment();
        m1.setUserId(1L);
        m1.setContent("今天第一次完成 100 个标准俯卧撑！坚持两个月终于突破了 💪");
        m1.setStatus(0);
        momentMapper.insert(m1);
        com.fitness.entity.Moment m2 = new com.fitness.entity.Moment();
        m2.setUserId(2L);
        m2.setContent("求助：硬拉的时候腰总是酸，是不是动作有问题？有经验的大佬指点一下");
        m2.setStatus(0);
        momentMapper.insert(m2);

        // 待审核课程评论（status=0）
        com.fitness.entity.CourseComment cc = new com.fitness.entity.CourseComment();
        cc.setCourseId(1L);
        cc.setUserId(2L);
        cc.setContent("这个课程强度对新手有点大，建议分两段");
        cc.setRating(4);
        cc.setStatus(0);
        courseCommentMapper.insert(cc);

        // 待审核动态评论（status=0）
        com.fitness.entity.MomentComment mc = new com.fitness.entity.MomentComment();
        mc.setMomentId(1L);
        mc.setUserId(2L);
        mc.setContent("一起加油！坚持就是胜利");
        mc.setStatus(0);
        momentCommentMapper.insert(mc);

        // 待审核 UGC 动作（status=1）
        Action ua = new Action();
        ua.setName("自重臂屈伸");
        ua.setPart("手臂");
        ua.setDifficulty(1);
        ua.setEquipment("双杠");
        ua.setSteps("双手撑杠，屈肘下降后推起，锻炼肱三头肌");
        ua.setStatus(1);
        actionMapper.insert(ua);

        // 待审核 UGC 课程（status=1）
        Course uc = new Course();
        uc.setName("居家零器械全身训练（用户投稿）");
        uc.setCategory("塑形");
        uc.setIntro("分享一套自己组合的居家训练，无需器械也能练全身");
        uc.setDifficulty(1);
        uc.setStatus(1);
        courseMapper.insert(uc);
    }

    // ==================== 辅助方法 ====================

    private Role insertRole(String name, String code, String desc) {
        Role r = new Role();
        r.setName(name);
        r.setCode(code);
        r.setDescription(desc);
        roleMapper.insert(r);
        return r;
    }

    private Permission insertPerm(String name, String code, int type, Long parentId, String path, long sort) {
        Permission p = new Permission();
        p.setName(name);
        p.setCode(code);
        p.setType(type);
        p.setParentId(parentId);
        p.setPath(path);
        p.setSort((int) sort);
        permissionMapper.insert(p);
        return p;
    }

    private RolePermission buildRolePerm(Long roleId, Long permId) {
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permId);
        return rp;
    }

    private void insertUser(String phone, String pwd, String nickname, int gender, int height, int goal, int level) {
        User u = new User();
        u.setPhone(phone);
        u.setPassword(encoder.encode(pwd));
        u.setNickname(nickname);
        u.setGender(gender);
        u.setHeight(new BigDecimal(height));
        u.setGoal(goal);
        u.setLevel(level);
        u.setTags(level == 3 ? (goal == 1 ? "增肌,进阶" : "减脂,进阶") : (goal == 1 ? "增肌,新手" : "减脂,新手"));
        u.setStatus(1);
        userMapper.insert(u);
    }

    private void insertAction(String name, String part, int difficulty, String equipment,
                              String steps, String tips, String errors, String breath, int mediaType, String mediaUrl) {
        Action a = new Action();
        a.setName(name);
        a.setPart(part);
        a.setDifficulty(difficulty);
        a.setEquipment(equipment);
        a.setSteps(steps);
        a.setTips(tips);
        a.setErrors(errors);
        a.setBreath(breath);
        a.setMediaType(mediaType);
        a.setMediaUrl(mediaUrl);
        a.setStatus(2); // 已发布
        actionMapper.insert(a);
    }

    private void insertFood(String name, String category, String calorie, String protein,
                            String carb, String fat, String unit) {
        Food f = new Food();
        f.setName(name);
        f.setCategory(category);
        f.setCalorie(new BigDecimal(calorie));
        f.setProtein(new BigDecimal(protein));
        f.setCarb(new BigDecimal(carb));
        f.setFat(new BigDecimal(fat));
        f.setUnit(unit);
        f.setStatus(1);
        foodMapper.insert(f);
    }

    private void insertRecipe(String name, int goal, String tags, String content,
                              int calorie, String protein, String carb, String fat, String cover) {
        Recipe r = new Recipe();
        r.setName(name);
        r.setGoal(goal);
        r.setTags(tags);
        r.setContent(content);
        r.setCalorie(calorie);
        r.setProtein(new BigDecimal(protein));
        r.setCarb(new BigDecimal(carb));
        r.setFat(new BigDecimal(fat));
        r.setCover(cover);
        r.setStatus(1);
        recipeMapper.insert(r);
    }

    private void insertCourse(String name, String category, String intro, int difficulty, int duration, int calorie) {
        Course c = new Course();
        c.setName(name);
        c.setCategory(category);
        c.setIntro(intro);
        c.setDifficulty(difficulty);
        c.setDuration(duration);
        c.setCalorie(calorie);
        c.setStatus(2); // 已发布
        courseMapper.insert(c);
    }

    private TrainingPlan insertPlan(Long userId, String name, int goal, int level, String cycle, String tags, int isTemplate) {
        TrainingPlan p = new TrainingPlan();
        p.setUserId(userId);
        p.setName(name);
        p.setGoal(goal);
        p.setLevel(level);
        p.setCycle(cycle);
        p.setTags(tags);
        p.setIsTemplate(isTemplate);
        planMapper.insert(p);
        return p;
    }

    private void bindAction(Long planId, Long actionId, int dayNo, int sets, int reps, int rest, int sort) {
        PlanAction pa = new PlanAction();
        pa.setPlanId(planId);
        pa.setActionId(actionId);
        pa.setDayNo(dayNo);
        pa.setSets(sets);
        pa.setReps(reps);
        pa.setRest(rest);
        pa.setSort(sort);
        planActionMapper.insert(pa);
    }

    private void insertArticle(String title, int type, String content, String cover) {
        Article a = new Article();
        a.setTitle(title);
        a.setType(type);
        a.setContent(content);
        a.setCover(cover);
        a.setStatus(1);
        articleMapper.insert(a);
    }
}
