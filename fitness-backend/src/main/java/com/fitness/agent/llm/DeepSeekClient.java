package com.fitness.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.agent.dto.AiCourseResult;
import com.fitness.agent.dto.IntentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 客户端（直接 HTTP 调 OpenAI 兼容接口，不引入 Spring AI）
 * <p>职责：意图识别 + 槽位抽取（JSON mode）、课程/计划自然语言说明生成、知识问答兜底。
 * 未配置 apiKey 时 isConfigured()=false，上层走规则引擎降级通道。
 */
@Component
public class DeepSeekClient {

    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(
            @Value("${ai.deepseek.api-key:}") String apiKey,
            @Value("${ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${ai.deepseek.model:deepseek-chat}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 意图识别 + 槽位抽取；失败返回 null（上层降级） */
    public IntentResult parseIntent(String input) {
        String system = """
                你是智慧健身系统的 AI 私教助手。请从用户话术中识别意图并抽取结构化槽位，只输出 JSON（不要输出任何 JSON 以外的内容）。
                意图类型：
                - recommend_course：用户想练一次/生成单次课程
                - create_plan：用户想制定多周训练计划
                - ask_question：用户在问健身知识
                - chat：闲聊或其他
                JSON 字段（无法判断的填 null）：
                {"intent":"recommend_course","goal":1,"level":1,"muscles":["胸"],"equipment":["哑铃"],"duration":30,"weeks":null,"question":null,"contraindications":["腰伤"]}
                goal：1增肌/2减脂/3塑形/4保持；level：1新手/2初级/3进阶；muscles 部位取 胸/背/肩/手臂/腿/核心/全身；equipment 取 无/哑铃/杠铃/绳索/固定器械/弹力带/史密斯机/壶铃 等；contraindications 取 腰伤/膝伤/肩伤/手腕伤 等伤病禁忌（用户未提及则填 null）。
                """;
        try {
            String raw = chat(system, input, true);
            if (raw == null || raw.isBlank()) return null;
            return objectMapper.readValue(raw, IntentResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 将结构化课程转为自然语言说明（仅润色，课程结构已由规则引擎确定） */
    public String describeCourse(AiCourseResult course) {
        String system = """
                你是智慧健身系统的 AI 私教助手。根据给定的结构化训练课程，用简洁、鼓励性的中文向用户说明本次训练安排，可简要说明训练目标与注意事项，控制在 150 字以内。
                """;
        try {
            return chat(system, "课程：" + objectMapper.valueToTree(course).toString(), false);
        } catch (Exception e) {
            return null;
        }
    }

    /** 知识问答 LLM 兜底（FAQ 未命中时），附免责声明 */
    public String answerQuestion(String question) {
        String system = """
                你是智慧健身系统的健身知识助手。请用专业、简洁的中文回答用户的健身问题。只提供一般性健身知识，不提供医疗诊断、用药建议；涉及伤病时引导用户就医。回答末尾可附一句"以上为一般性建议，具体请结合自身情况或咨询专业人士"。
                """;
        try {
            return chat(system, question, false);
        } catch (Exception e) {
            return null;
        }
    }

    /** 闲聊回应（独立 prompt，不带免责声明） */
    public String chatReply(String input) {
        String system = """
                你是智慧健身系统的 AI 私教助手，友好、热情。请用简洁的中文回应用户，并自然引导其使用课程生成或知识问答功能（例如「我只有哑铃想练胸 30 分钟」）。
                """;
        try {
            return chat(system, input, false);
        } catch (Exception e) {
            return null;
        }
    }

    /** 学习计划周期化说明 */
    public String describePlan(String planJson) {
        String system = """
                你是智慧健身系统的 AI 私教助手。根据给定的多周训练计划结构，用简洁中文说明该计划的周期化安排（适应期/递增期/减载期）与渐进超负荷思路，150 字以内。
                """;
        try {
            return chat(system, planJson, false);
        } catch (Exception e) {
            return null;
        }
    }

    /** 第二层安全分类（LLM，防同音/绕写绕过关键词）；失败返回 false（不误伤） */
    public boolean isUnsafe(String input) {
        String system = """
                你是健身内容安全分类器。判断用户输入是否涉及以下高风险话题：伤病求医问药、用药/补剂滥用、极端减食（催吐/绝食/一天只吃一顿）、未成年人训练、孕产训练。只输出 JSON，格式 {"unsafe":true} 或 {"unsafe":false}，不要输出其他内容。
                """;
        try {
            String raw = chat(system, input, true);
            if (raw == null || raw.isBlank()) return false;
            JsonNode node = objectMapper.readTree(raw);
            return node.path("unsafe").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private String chat(String system, String user, boolean jsonMode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user == null ? "" : user)));
        body.put("temperature", 0.1);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        String resp = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(resp);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            return null;
        }
    }
}
