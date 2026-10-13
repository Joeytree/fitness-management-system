package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * knowledge_qa 表实体：AI 私教健身知识 FAQ
 */
@Data
@TableName("knowledge_qa")
public class KnowledgeQa {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 分类：训练原理/动作教学/营养基础/恢复知识/器材使用 */
    private String category;
    private String question;
    /** 关键词，逗号分隔 */
    private String keywords;
    private String answer;
    /** 来源文档 */
    private String source;
    /** 1上架/0下架 */
    private Integer status;
    private LocalDateTime createTime;
}
