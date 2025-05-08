-- 创建数据库
CREATE DATABASE IF NOT EXISTS sageblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE sageblog;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态 0:待审核 1:正常 2:禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关系表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父级ID',
  `name` varchar(50) NOT NULL COMMENT '权限名称',
  `code` varchar(50) NOT NULL COMMENT '权限编码',
  `type` tinyint(4) NOT NULL COMMENT '类型 1:菜单 2:按钮 3:接口',
  `url` varchar(255) DEFAULT NULL COMMENT '菜单URL或接口URL',
  `method` varchar(10) DEFAULT NULL COMMENT 'HTTP方法，接口权限特有',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可见',
  `description` varchar(255) DEFAULT NULL COMMENT '权限描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 角色权限关系表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `permission_id` bigint(20) NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表';

-- 注册审核记录表
CREATE TABLE IF NOT EXISTS `sys_register_audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '审核状态 0:待审核 1:通过 2:拒绝',
  `remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册审核记录表';

-- 系统日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` varchar(50) DEFAULT NULL COMMENT '操作内容',
  `method` varchar(255) DEFAULT NULL COMMENT '操作方法',
  `params` text DEFAULT NULL COMMENT '操作参数',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `status` tinyint(4) DEFAULT NULL COMMENT '操作状态 0:失败 1:成功',
  `error_msg` text DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- 初始化角色数据
INSERT INTO `sys_role` (`name`, `code`, `description`) VALUES 
('超级管理员', 'ROLE_SUPER_ADMIN', '系统超级管理员，拥有所有权限'),
('管理员', 'ROLE_ADMIN', '系统管理员，可管理大部分功能'),
('普通用户', 'ROLE_USER', '普通用户，基础功能');

-- 初始化权限数据 - 菜单类型
INSERT INTO `sys_permission` (`parent_id`, `name`, `code`, `type`, `url`, `icon`, `sort`, `visible`, `description`) VALUES 
(NULL, '系统管理', 'system:manage', 1, '/system', 'el-icon-setting', 1, 1, '系统管理菜单'),
(1, '用户管理', 'system:user', 1, '/system/user', 'el-icon-user', 1, 1, '用户管理菜单'),
(1, '角色管理', 'system:role', 1, '/system/role', 'el-icon-s-check', 2, 1, '角色管理菜单'),
(1, '权限管理', 'system:permission', 1, '/system/permission', 'el-icon-key', 3, 1, '权限管理菜单'),
(1, '菜单管理', 'system:menu', 1, '/system/menu', 'el-icon-menu', 4, 1, '菜单管理菜单'),
(1, '审核管理', 'system:audit', 1, '/system/audit', 'el-icon-s-check', 5, 1, '审核管理菜单'),
(NULL, '个人中心', 'user:center', 1, '/user/center', 'el-icon-user', 2, 1, '个人中心菜单');

-- 初始化权限数据 - 功能权限
INSERT INTO `sys_permission` (`parent_id`, `name`, `code`, `type`, `url`, `method`, `visible`, `description`) VALUES 
(2, '用户列表', 'system:user:list', 3, '/api/users', 'GET', 1, '获取用户列表'),
(2, '添加用户', 'system:user:add', 3, '/api/users', 'POST', 1, '添加用户'),
(2, '修改用户', 'system:user:edit', 3, '/api/users/{id}', 'PUT', 1, '修改用户'),
(2, '删除用户', 'system:user:delete', 3, '/api/users/{id}', 'DELETE', 1, '删除用户'),
(2, '查看用户', 'system:user:view', 3, '/api/users/{id}', 'GET', 1, '查看用户'),
(3, '角色列表', 'system:role:list', 3, '/api/roles', 'GET', 1, '获取角色列表'),
(3, '添加角色', 'system:role:add', 3, '/api/roles', 'POST', 1, '添加角色'),
(3, '修改角色', 'system:role:edit', 3, '/api/roles/{id}', 'PUT', 1, '修改角色'),
(3, '删除角色', 'system:role:delete', 3, '/api/roles/{id}', 'DELETE', 1, '删除角色'),
(3, '角色授权', 'system:role:authorize', 3, '/api/roles/{id}/permissions', 'POST', 1, '角色授权'),
(4, '权限列表', 'system:permission:list', 3, '/api/permissions', 'GET', 1, '获取权限列表'),
(4, '添加权限', 'system:permission:add', 3, '/api/permissions', 'POST', 1, '添加权限'),
(4, '修改权限', 'system:permission:edit', 3, '/api/permissions/{id}', 'PUT', 1, '修改权限'),
(4, '删除权限', 'system:permission:delete', 3, '/api/permissions/{id}', 'DELETE', 1, '删除权限'),
(5, '菜单列表', 'system:menu:list', 3, '/api/menus', 'GET', 1, '获取菜单列表'),
(5, '添加菜单', 'system:menu:add', 3, '/api/menus', 'POST', 1, '添加菜单'),
(5, '修改菜单', 'system:menu:edit', 3, '/api/menus/{id}', 'PUT', 1, '修改菜单'),
(5, '删除菜单', 'system:menu:delete', 3, '/api/menus/{id}', 'DELETE', 1, '删除菜单'),
(6, '审核列表', 'system:audit:list', 3, '/api/audits', 'GET', 1, '获取审核列表'),
(6, '审核操作', 'system:audit:operation', 3, '/api/audits/{id}', 'POST', 1, '执行审核操作'),
(7, '个人信息', 'user:info', 3, '/api/users/info', 'GET', 1, '获取个人信息'),
(7, '修改信息', 'user:info:update', 3, '/api/users/info', 'PUT', 1, '修改个人信息'),
(7, '修改密码', 'user:password:update', 3, '/api/users/password', 'PUT', 1, '修改密码');

-- 初始化角色权限关系 - 超级管理员拥有所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission`;

-- 管理员拥有除权限管理外的所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, id FROM `sys_permission` WHERE code NOT LIKE 'system:permission%';

-- 普通用户仅拥有个人中心相关权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 3, id FROM `sys_permission` WHERE code LIKE 'user:%'; 