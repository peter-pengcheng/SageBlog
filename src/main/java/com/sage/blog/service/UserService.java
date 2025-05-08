package com.sage.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sage.blog.entity.User;
import com.sage.blog.model.ProfileUpdateDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService extends IService<User> {

    User findByEmail(String email);

    /**
     * 注册用户
     *
     * @param user 用户信息
     * @return 是否成功
     */
    boolean register(User user);

    /**
     * 更新用户状态
     *
     * @param userId     用户ID
     * @param status     状态
     * @param operatorId 操作人ID
     * @param remark     备注
     * @return 是否成功
     */
    boolean updateUserStatus(Long userId, Integer status, Long operatorId, String remark);

    /**
     * 分页查询用户列表
     *
     * @param page     页码
     * @param size     每页数量
     * @param username 用户名（模糊查询）
     * @param status   状态
     * @return 用户分页数据
     */
    IPage<User> getUserPage(int page, int size, String username, Integer status);

    /**
     * 获取待审核用户列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 待审核用户分页数据
     */
    IPage<User> getPendingAuditUserPage(int page, int size);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    User getUserByEmail(String email);

    /**
     * 更新用户最后登录时间
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean updateLastLoginTime(Long userId);

    /**
     * 给用户分配角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean assignRoles(Long userId, List<Long> roleIds);

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    User findByUsername(String username);

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    User findById(Long id);

    /**
     * 更新用户个人资料
     *
     * @param userId     用户ID
     * @param profileDto 个人资料数据
     * @return 更新后的用户对象
     */
    User updateProfile(Long userId, ProfileUpdateDto profileDto);

    /**
     * 更新用户密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否更新成功
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 更新用户头像
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return 头像URL
     */
    String updateAvatar(Long userId, MultipartFile file);

    /**
     * 创建密码重置令牌
     *
     * @param email 用户邮箱
     * @return 重置令牌，如果用户不存在则返回null
     */
    String createPasswordResetToken(String email);

    /**
     * 验证密码重置令牌
     *
     * @param token 重置令牌
     * @return 令牌对应的用户ID，如果令牌无效则返回null
     */
    Long validatePasswordResetToken(String token);

    /**
     * 重置用户密码
     *
     * @param token       重置令牌
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPassword(String token, String newPassword);
}