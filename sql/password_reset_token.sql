-- 创建密码重置令牌表
CREATE TABLE IF NOT EXISTS `sys_password_reset_token` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `token` varchar(100) NOT NULL COMMENT '令牌',
  `expiry_date` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0-未使用，1-已使用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置令牌表'; 