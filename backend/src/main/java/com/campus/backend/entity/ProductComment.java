package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductComment {

    private Long id;

    private Long productId;

    private Long userId;

    private Long parentId;

    private String content;

    private Integer likeCount;

    private String status;

    private LocalDateTime createdAt;
}
