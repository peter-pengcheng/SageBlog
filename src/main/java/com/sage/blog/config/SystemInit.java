package com.sage.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sage.blog.entity.Role;
import com.sage.blog.entity.User;
import com.sage.blog.entity.UserRole;
import com.sage.blog.mapper.RoleMapper;
import com.sage.blog.mapper.UserMapper;
import com.sage.blog.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 系统初始化，创建默认管理员用户
 */
@Component
public class SystemInit implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SystemInit.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${sageblog.init.admin.username}")
    private String adminUsername;

    @Value("${sageblog.init.admin.password}")
    private String adminPassword;

    @Value("${sageblog.init.admin.email}")
    private String adminEmail;

    @Value("${sageblog.file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) throws Exception {
        // 初始化文件上传目录
        initUploadDirs();

        // 检查管理员用户是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", adminUsername);
        User admin = userMapper.selectOne(queryWrapper);

        if (admin == null) {
            // 创建管理员用户
            admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setNickname("系统管理员");
            admin.setEmail(adminEmail);
            admin.setStatus(1); // 正常状态
            admin.setCreateTime(LocalDateTime.now());
            admin.setAvatar("/sageblog/resources/avatars/default-avatar.jpg");
            userMapper.insert(admin);

            // 检查管理员角色是否存在
            QueryWrapper<Role> roleQuery = new QueryWrapper<>();
            roleQuery.eq("code", "ROLE_ADMIN");
            Role adminRole = roleMapper.selectOne(roleQuery);

            if (adminRole == null) {
                // 创建管理员角色
                adminRole = new Role();
                adminRole.setName("管理员");
                adminRole.setCode("ROLE_ADMIN");
                adminRole.setDescription("系统管理员角色");
                adminRole.setCreateTime(LocalDateTime.now());
                roleMapper.insert(adminRole);
            }

            // 分配角色给管理员
            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(adminRole.getId());
            userRole.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(userRole);

            logger.info("系统初始化完成，已创建管理员用户: {}", adminUsername);
        }
    }

    /**
     * 初始化文件上传目录
     */
    private void initUploadDirs() {
        try {
            // 创建上传根目录
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("已创建上传根目录: {}", uploadPath);
            }

            // 创建头像目录
            Path avatarPath = Paths.get(uploadDir, "avatars");
            if (!Files.exists(avatarPath)) {
                Files.createDirectories(avatarPath);
                logger.info("已创建头像目录: {}", avatarPath);
            }

            // 创建默认头像
            File defaultAvatar = new File(avatarPath.toString(), "default-avatar.jpg");
            if (!defaultAvatar.exists()) {
                try (InputStream is = new ClassPathResource("static/img/default-avatar.jpg").getInputStream()) {
                    FileCopyUtils.copy(is, Files.newOutputStream(defaultAvatar.toPath()));
                    logger.info("已创建默认头像: {}", defaultAvatar);
                } catch (IOException e) {
                    logger.error("创建默认头像失败", e);
                }
            }
        } catch (IOException e) {
            logger.error("初始化上传目录失败", e);
        }
    }
}