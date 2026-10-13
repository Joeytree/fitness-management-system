package com.fitness.agent.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.KnowledgeQa;
import com.fitness.mapper.KnowledgeQaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * FAQ 知识检索（MySQL 结构化检索，非向量库）
 * <p>知识量小（几十条），全量加载后做 精确匹配 + 关键词匹配，可解释、零新增组件。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeSearch {

    private final KnowledgeQaMapper knowledgeQaMapper;

    /** 检索 FAQ，命中返回标准答复，未命中返回 null（上层走 LLM 兜底） */
    public KnowledgeQa search(String query) {
        if (query == null || query.isBlank()) return null;
        String q = query.trim();
        List<KnowledgeQa> all = knowledgeQaMapper.selectList(
                new LambdaQueryWrapper<KnowledgeQa>().eq(KnowledgeQa::getStatus, 1));

        // 1. 精确匹配：问题完全一致，或问题被查询包含
        for (KnowledgeQa qa : all) {
            if (qa.getQuestion() != null && (qa.getQuestion().equals(q) || q.contains(qa.getQuestion()))) {
                return qa;
            }
        }
        // 2. 关键词匹配：取命中关键词数最多的 FAQ（提高相关性，避免宽泛词误命中）
        KnowledgeQa best = null;
        int bestScore = 0;
        for (KnowledgeQa qa : all) {
            if (qa.getKeywords() == null) continue;
            int score = 0;
            for (String k : qa.getKeywords().split(",")) {
                String kw = k.trim();
                if (!kw.isEmpty() && q.contains(kw)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = qa;
            }
        }
        return best;
    }
}
