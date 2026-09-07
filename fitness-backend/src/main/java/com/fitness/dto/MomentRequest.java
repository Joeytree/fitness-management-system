package com.fitness.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MomentRequest {

    @NotBlank(message = "动态内容不能为空")
    @Size(max = 500, message = "动态内容不能超过500字")
    private String content;

    @Size(max = 9, message = "最多上传9张图片")
    private List<String> images;
}
