package com.sage.blog.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件服务Servlet
 * 直接提供文件访问，绕过Spring MVC控制器机制
 */
@WebServlet(urlPatterns = { "/resources/*", "/sageblog/resources/*" })
@Component
public class FileServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);
    private String uploadDir = "./uploads"; // 默认值
    private File uploadsDirectory;

    /**
     * 在Servlet容器初始化时设置目录路径
     */
    @PostConstruct
    public void init() {
        logger.info("初始化文件服务Servlet...");

        // 打印当前工作目录，帮助诊断路径问题
        logger.info("当前工作目录: {}", new File(".").getAbsolutePath());

        // 初始化上传目录 - 使用绝对路径处理
        uploadsDirectory = new File(uploadDir).getAbsoluteFile();
        logger.info("上传目录配置值: {}", uploadDir);
        logger.info("上传目录解析为: {}", uploadsDirectory.getAbsolutePath());

        if (!uploadsDirectory.exists()) {
            boolean created = uploadsDirectory.mkdirs();
            logger.info("上传目录创建结果: {}", created);
        }

        // 打印目录详情
        logger.info("上传目录路径: {}", uploadsDirectory.getAbsolutePath());
        logger.info("上传目录是否存在: {}", uploadsDirectory.exists());
        logger.info("上传目录是否可读: {}", uploadsDirectory.canRead());

        File[] files = uploadsDirectory.listFiles();
        if (files != null) {
            logger.info("目录中包含 {} 个文件", files.length);
            for (File file : files) {
                logger.info(" - 文件: {}, 大小: {} 字节, 可读: {}",
                        file.getName(), file.length(), file.canRead());
            }
        } else {
            logger.warn("无法列出上传目录中的文件，可能是权限问题或目录不存在");
        }

        logger.info("文件服务Servlet初始化完成");
    }

    /**
     * 直接处理文件请求
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 提取路径信息
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();

        logger.info("===== 文件请求 =====");
        logger.info("请求URI: {}", requestURI);
        logger.info("上下文路径: {}", contextPath);
        logger.info("Servlet路径: {}", servletPath);
        logger.info("请求URL: {}", request.getRequestURL());
        logger.info("查询字符串: {}", request.getQueryString());

        // 提取文件名
        String filename;
        if (requestURI.contains("/resources/")) {
            filename = requestURI.substring(requestURI.indexOf("/resources/") + "/resources/".length());
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "无效的资源路径");
            return;
        }

        logger.info("请求文件名: {}", filename);

        // 安全检查 - 确保没有路径遍历
        if (filename.contains("..") || filename.contains("\\")) {
            logger.error("检测到不安全的文件路径: {}", filename);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "不允许的文件路径");
            return;
        }

        // 构建文件路径并检查文件是否存在
        File file = new File(uploadsDirectory, filename);

        logger.info("文件完整路径: {}", file.getAbsolutePath());
        logger.info("文件是否存在: {}", file.exists());
        logger.info("文件是否可读: {}", file.canRead());
        logger.info("文件大小: {}", file.exists() ? file.length() : "N/A");

        if (!file.exists() || !file.isFile()) {
            // 尝试不区分大小写查找文件
            logger.info("文件不存在，尝试不区分大小写查找");
            File[] allFiles = uploadsDirectory.listFiles();
            if (allFiles != null) {
                for (File f : allFiles) {
                    if (f.getName().equalsIgnoreCase(filename)) {
                        logger.info("找到匹配文件: {}", f.getName());
                        file = f;
                        break;
                    }
                }
            }

            // 如果仍未找到，尝试在子目录中查找
            if (!file.exists() || !file.isFile()) {
                logger.info("在主目录中未找到文件，尝试在子目录中查找");
                File found = searchFileInDirectory(uploadsDirectory, filename);
                if (found != null) {
                    logger.info("在子目录中找到文件: {}", found.getAbsolutePath());
                    file = found;
                }
            }

            // 再次检查
            if (!file.exists() || !file.isFile()) {
                logger.error("文件最终未找到: {}", filename);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }
        }

        // 设置内容类型
        String contentType = getContentType(file);
        response.setContentType(contentType);
        logger.info("设置内容类型: {}", contentType);

        // 设置缓存控制
        response.setHeader("Cache-Control", "max-age=86400"); // 1天缓存
        response.setHeader("Expires", "86400");

        // 设置内容长度
        response.setContentLength((int) file.length());

        // 使用缓冲复制文件内容到响应输出流
        try (FileInputStream input = new FileInputStream(file);
                OutputStream output = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            logger.info("文件发送成功: {}, 大小: {} 字节", filename, file.length());
        } catch (IOException e) {
            logger.error("发送文件时出错: {}", e.getMessage(), e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "读取文件时出错");
            }
        }
    }

    /**
     * 递归搜索文件
     */
    private File searchFileInDirectory(File directory, String filename) {
        if (directory == null || !directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().equalsIgnoreCase(filename)) {
                return file;
            } else if (file.isDirectory()) {
                File found = searchFileInDirectory(file, filename);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * 根据文件确定内容类型
     */
    private String getContentType(File file) {
        try {
            // 先尝试通过Files.probeContentType判断
            Path path = file.toPath();
            String contentType = Files.probeContentType(path);
            if (contentType != null && !contentType.isEmpty()) {
                return contentType;
            }
        } catch (IOException e) {
            logger.warn("无法探测文件类型: {}", e.getMessage());
        }

        // 根据扩展名判断
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".pdf")) {
            return "application/pdf";
        } else if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "text/html";
        } else if (name.endsWith(".css")) {
            return "text/css";
        } else if (name.endsWith(".js")) {
            return "application/javascript";
        } else if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }

        // 默认二进制
        return "application/octet-stream";
    }

    /**
     * 设置上传目录路径
     */
    @Value("${sageblog.file.upload-dir}")
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
        logger.info("设置上传目录: {}", uploadDir);
    }
}