package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类信息VO (返回前端，支持树形结构)
 */
@Data
public class CategoryVO {

    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 子分类列表 (树形结构) */
    private List<CategoryVO> children;

    /** 获取状态文本 */
    public String getStatusText() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "启用";
            default -> "未知";
        };
    }
}
