package com.sage.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sage.blog.entity.RegisterAudit;

public interface RegisterAuditService extends IService<RegisterAudit> {

    /**
     * 分页查询审核记录
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 审核状态
     * @return 审核记录分页数据
     */
    IPage<RegisterAudit> getAuditPage(int page, int size, Integer status);

    /**
     * 审核用户注册
     *
     * @param auditId   审核记录ID
     * @param status    审核状态
     * @param remark    备注
     * @param auditorId 审核人ID
     * @return 是否成功
     */
    boolean auditRegister(Long auditId, Integer status, String remark, Long auditorId);
}