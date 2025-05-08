package com.sage.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AuditRequest {

    @NotNull(message = "审核ID不能为空")
    private Long auditId;

    @NotNull(message = "审核状态不能为空")
    private Integer status;

    private String remark;
}