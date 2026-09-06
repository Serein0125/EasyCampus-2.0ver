package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserLike {

    private Long id;

    private Long userId;

    private String targetType;

    private Long targetId;

    private LocalDateTime createdAt;
}
