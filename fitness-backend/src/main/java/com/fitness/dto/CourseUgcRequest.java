package com.fitness.dto;

import lombok.Data;

import java.util.List;

/**
 * UGC 自定义课程提交/编辑请求（含封面 + 动作编排 + 可见性）
 */
@Data
public class CourseUgcRequest {

    private String name;
    private String category;
    private String part;
    /** 封面上传（相对路径 /upload/...，可选） */
    private String cover;
    private String intro;
    private Integer difficulty;
    private Integer duration;
    private Integer calorie;
    /** 可见性：private=仅自用 / public=申请公开，默认 public */
    private String visibility;
    /** 动作编排明细（按数组顺序赋 sort） */
    private List<CourseActionItem> actionItems;
}
