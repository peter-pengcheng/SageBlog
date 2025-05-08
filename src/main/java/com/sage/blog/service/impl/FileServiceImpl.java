package com.sage.blog.service.impl;

import com.sage.blog.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件服务实现类
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Value("${sageblog.file.upload-dir}")
    private String uploadDir;

    @Value("${sageblog.file.avatar-url-prefix}")
    private String avatarUrlPrefix;

    @Override
    public String uploadAvatar(MultipartFile file, String username) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("上传的文件为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("只允许上传图片类型的文件");
        }

        // 验证文件大小 (限制为2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IOException("文件大小不能超过2MB");
        }

        // 创建保存目录
        String avatarDir = uploadDir + File.separator + "avatars";
        createDirIfNotExists(avatarDir);

        // 生成唯一文件名 (用户名+UUID+原始文件扩展名)
        String originalFilename = file.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        if (fileExtension == null) {
            fileExtension = "jpg";
        }
        String filename = username + "_" + UUID.randomUUID().toString() + "." + fileExtension;

        // 保存文件
        Path targetLocation = Paths.get(avatarDir + File.separator + filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // 返回文件URL
        return avatarUrlPrefix + "/avatars/" + filename;
    }

    /**
     * 如果目录不存在，创建它
     */
    private void createDirIfNotExists(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                logger.error("无法创建目录: {}", dir);
            }
        }
    }
}