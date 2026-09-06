package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserBehavior {

    private Long id;

    private Long userId;

    private String behaviorType;

    private String targetType;

    private Long targetId;

    private LocalDateTime createdAt;
}
