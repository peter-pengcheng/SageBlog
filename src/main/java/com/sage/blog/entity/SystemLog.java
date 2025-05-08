package com.sage.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class SystemLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String operation;

    private String method;

    private String params;

    private String ip;

    /**
     * 操作状态：0-失败，1-成功
     */
    private Integer status;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}