package com.sage.blog.service.impl;

import com.sage.blog.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${sageblog.mail.from}")
    private String from;

    @Value("${sageblog.mail.admin-notification}")
    private boolean adminNotification;

    @Value("${sageblog.base-url}")
    private String baseUrl;

    @Override
    public boolean sendSimpleMail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            logger.error("发送简单邮件失败", e);
            return false;
        }
    }

    @Override
    public boolean sendHtmlMail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            logger.error("发送HTML邮件失败", e);
            return false;
        }
    }

    @Override
    public boolean sendRegisterSuccessMail(String to, String username) {
        String subject = "注册成功通知 - SageBlog";

        Context context = new Context();
        context.setVariable("username", username);
        String content = templateEngine.process("mail/register-success", context);

        return sendHtmlMail(to, subject, content);
    }

    @Override
    public boolean sendRegisterApprovedMail(String to, String username) {
        String subject = "注册审核通过通知 - SageBlog";

        Context context = new Context();
        context.setVariable("username", username);
        String content = templateEngine.process("mail/register-approved", context);

        return sendHtmlMail(to, subject, content);
    }

    @Override
    public boolean sendRegisterRejectedMail(String to, String username, String reason) {
        String subject = "注册审核拒绝通知 - SageBlog";

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("reason", reason);
        String content = templateEngine.process("mail/register-rejected", context);

        return sendHtmlMail(to, subject, content);
    }

    @Override
    public boolean sendPendingAuditNotification(String adminEmail, String username) {
        // 如果没有开启管理员通知，则直接返回成功
        if (!adminNotification) {
            return true;
        }

        String subject = "新用户注册待审核通知 - SageBlog";

        Context context = new Context();
        context.setVariable("username", username);
        String content = templateEngine.process("mail/pending-audit", context);

        return sendHtmlMail(adminEmail, subject, content);
    }

    @Override
    public boolean sendPasswordResetMail(String to, String username, String token, String resetUrl) {
        String subject = "密码重置请求 - SageBlog";

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("token", token);
        String content = templateEngine.process("mail/password-reset", context);

        return sendHtmlMail(to, subject, content);
    }

    @Override
    public boolean sendPasswordResetEmail(String to, String token, String username) {
        String subject = "密码重置 - SageBlog";

        // 构建重置密码的链接
        String resetUrl = baseUrl + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("token", token);
        String content = templateEngine.process("mail/password-reset", context);

        try {
            sendHtmlMail(to, subject, content);
            return true;
        } catch (Exception e) {
            logger.error("发送密码重置邮件失败: {}", to, e);
            return false;
        }
    }
}