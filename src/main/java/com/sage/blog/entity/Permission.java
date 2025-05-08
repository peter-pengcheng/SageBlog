package com.sage.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private String code;

    /**
     * 类型：1-菜单，2-按钮，3-接口
     */
    private Integer type;

    private String url;

    private String method;

    private String icon;

    private Integer sort;

    private Boolean visible;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /**
     * 子节点列表，数据库不存储
     */
    @TableField(exist = false)
    private List<Permission> children;
}