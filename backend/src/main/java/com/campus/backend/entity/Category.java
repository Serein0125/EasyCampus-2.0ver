package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品分类实体类 (树形结构)
 * 对应数据库 categories 表
 */
@Data
public class Category {

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description;

    /** 图标URL */
    private String iconUrl;

    /** 父分类ID (NULL表示根分类) */
    private Long parentId;

    /** 排序序号 (越小越靠前) */
    private Integer sortOrder;

    /** 状态: 0禁用, 1启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
