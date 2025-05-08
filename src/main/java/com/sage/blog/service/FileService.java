package com.sage.blog.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传用户头像
     *
     * @param file     头像文件
     * @param username 用户名
     * @return 头像URL
     * @throws IOException 如果上传失败
     */
    String uploadAvatar(MultipartFile file, String username) throws IOException;
}