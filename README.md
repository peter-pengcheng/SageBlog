# SageBlog 用户管理系统

一个基于Spring Boot的用户管理系统，包含用户注册审核、RBAC权限控制、JWT认证等功能。

## 功能特性

- **注册审核机制**：用户注册后，需要经过管理员审核后才能获得正常使用权限
- **基于JWT的认证**：采用Spring Security + JWT的方式进行认证和授权
- **RBAC权限控制**：支持细粒度的权限控制，包括菜单访问权限和功能操作权限
- **邮件通知**：支持各类操作的邮件通知

## 技术架构

- 后端：Java 1.8 + Spring Boot 2.3.x
- 前端：Thymeleaf 模板引擎
- 数据库：MySQL + MyBatis Plus
- 缓存：Redis
- 认证：Spring Security + JWT
- 通知：Spring Mail

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis

### 配置说明

1. 数据库配置：修改 `application.yml` 中的数据库连接信息
2. Redis配置：修改 `application.yml` 中的Redis连接信息
3. 邮件配置：修改 `application.yml` 中的邮件服务器配置

### 初始化数据库

执行 `sql/init.sql` 脚本初始化数据库和表结构

### 运行项目

```bash
mvn spring-boot:run
```

系统会自动初始化一个管理员账号：

- 用户名：admin
- 密码：admin123
- 邮箱：admin@example.com

### 接口说明

#### 认证相关

- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/refresh` - 刷新令牌

#### 用户管理

- `GET /api/users` - 获取用户列表
- `GET /api/users/{id}` - 获取用户详情
- `GET /api/users/info` - 获取当前用户信息
- `PUT /api/users/info` - 更新当前用户信息
- `POST /api/users/{userId}/roles` - 分配用户角色
- `PUT /api/users/{userId}/status` - 更新用户状态
- `DELETE /api/users/{id}` - 删除用户

#### 审核管理

- `GET /api/audits` - 获取审核记录列表
- `GET /api/audits/pending-users` - 获取待审核用户列表
- `POST /api/audits` - 执行审核操作

## 数据库设计

系统主要包含以下数据表：

- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_permission` - 权限表
- `sys_user_role` - 用户角色关系表
- `sys_role_permission` - 角色权限关系表
- `sys_register_audit` - 注册审核记录表
- `sys_log` - 系统日志表

## 权限控制

系统采用RBAC（基于角色的访问控制）模型：

- 用户分配角色
- 角色分配权限
- 权限包括菜单访问权限和功能操作权限

## 许可证

MIT
