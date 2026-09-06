package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserFollow {

    private Long id;

    private Long followerId;

    private Long followeeId;

    private LocalDateTime createdAt;
}
