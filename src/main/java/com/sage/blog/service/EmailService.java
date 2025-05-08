package com.sage.blog.service;

public interface EmailService {

    /**
     * 发送简单文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     * @return 是否成功
     */
    boolean sendSimpleMail(String to, String subject, String content);

    /**
     * 发送HTML邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content HTML内容
     * @return 是否成功
     */
    boolean sendHtmlMail(String to, String subject, String content);

    /**
     * 发送注册成功邮件
     *
     * @param to       收件人
     * @param username 用户名
     * @return 是否成功
     */
    boolean sendRegisterSuccessMail(String to, String username);

    /**
     * 发送注册审核通过邮件
     *
     * @param to       收件人
     * @param username 用户名
     * @return 是否成功
     */
    boolean sendRegisterApprovedMail(String to, String username);

    /**
     * 发送注册审核拒绝邮件
     *
     * @param to       收件人
     * @param username 用户名
     * @param reason   拒绝原因
     * @return 是否成功
     */
    boolean sendRegisterRejectedMail(String to, String username, String reason);

    /**
     * 发送待审核用户通知给管理员
     *
     * @param adminEmail 管理员邮箱
     * @param username   用户名
     * @return 是否成功
     */
    boolean sendPendingAuditNotification(String adminEmail, String username);

    /**
     * 发送密码重置邮件
     *
     * @param to       收件人
     * @param username 用户名
     * @param token    重置令牌
     * @param resetUrl 重置链接
     * @return 是否成功
     */
    boolean sendPasswordResetMail(String to, String username, String token, String resetUrl);

    /**
     * 发送密码重置邮件
     *
     * @param to       收件人邮箱
     * @param token    重置密码的令牌
     * @param username 用户名
     * @return 是否发送成功
     */
    boolean sendPasswordResetEmail(String to, String token, String username);
}