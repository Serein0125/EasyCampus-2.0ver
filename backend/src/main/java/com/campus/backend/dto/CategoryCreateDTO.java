package com.campus.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建分类请求DTO
 */
@Data
public class CategoryCreateDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 2, max = 50, message = "分类名称长度必须在2-50个字符之间")
    private String name;

    @Size(max = 200, message = "分类描述不能超过200个字符")
    private String description;

    /** 图标URL */
    private String iconUrl;

    /** 排序序号 */
    private Integer sortOrder;

    /** 父分类ID (NULL表示根分类) */
    private Long parentId;
}
