package com.sage.blog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 资源测试控制器
 * 用于调试和验证资源访问
 */
@Controller
@RequestMapping("/api/test")
public class ResourceTestController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTestController.class);

    @Value("${sageblog.file.upload-dir}")
    private String uploadDir;

    /**
     * 查看上传目录的文件列表
     * 用于检查服务器端文件是否存在
     */
    @GetMapping("/files")
    @ResponseBody
    public Map<String, Object> listFiles() {
        logger.info("列出上传目录文件");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> files = new ArrayList<>();

        try {
            File directory = new File(uploadDir);
            if (directory.exists() && directory.isDirectory()) {
                result.put("uploadDir", directory.getAbsolutePath());
                result.put("exists", true);

                File[] fileList = directory.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("path", file.getAbsolutePath());
                        fileInfo.put("size", file.length());
                        fileInfo.put("isDirectory", file.isDirectory());
                        fileInfo.put("lastModified", file.lastModified());
                        fileInfo.put("canRead", file.canRead());

                        // 构建URL
                        String url = "/resources/" + file.getName();
                        fileInfo.put("url", url);

                        files.add(fileInfo);
                    }
                }

                result.put("files", files);
                result.put("count", files.size());
                result.put("success", true);
            } else {
                result.put("exists", false);
                result.put("uploadDir", uploadDir);
                result.put("error", "上传目录不存在或不是目录");
                result.put("success", false);
            }
        } catch (Exception e) {
            logger.error("列出文件时出错", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 检查特定文件是否存在
     */
    @GetMapping("/check-file")
    @ResponseBody
    public Map<String, Object> checkFile(String filename) {
        Map<String, Object> result = new HashMap<>();

        if (filename == null || filename.isEmpty()) {
            result.put("success", false);
            result.put("error", "文件名不能为空");
            return result;
        }

        try {
            File file = new File(uploadDir, filename);
            result.put("filename", filename);
            result.put("exists", file.exists());
            result.put("path", file.getAbsolutePath());

            if (file.exists()) {
                result.put("isFile", file.isFile());
                result.put("size", file.length());
                result.put("canRead", file.canRead());
                result.put("url", "/resources/" + filename);
            }

            result.put("success", true);
        } catch (Exception e) {
            logger.error("检查文件时出错", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}