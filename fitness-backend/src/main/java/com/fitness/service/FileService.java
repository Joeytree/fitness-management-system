package com.fitness.service;

import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务（用户端 / 管理端共用）
 */
@Service
public class FileService {

    /** 允许上传的后缀白名单（图片/动图/视频），防止 .html/.svg/.js 等被当作活动内容解析（ISS-019） */
    private static final Set<String> ALLOWED_EXT = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".mp4", ".mov", ".m4v", ".avi");

    @Value("${fitness.upload.dir}")
    private String uploadDir;

    public Map<String, String> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件为空");
        }
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(dir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败");
        }

        Map<String, String> data = new HashMap<>();
        data.put("url", "/upload/" + filename);
        return data;
    }
}
