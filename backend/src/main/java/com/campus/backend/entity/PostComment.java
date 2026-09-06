package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostComment {

    private Long id;

    private Long postId;

    private Long userId;

    private Long parentId;

    private String content;

    private Integer likeCount;

    private String status;

    private LocalDateTime createdAt;
}
