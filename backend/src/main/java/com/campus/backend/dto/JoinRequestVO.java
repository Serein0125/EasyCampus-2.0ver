package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JoinRequestVO {
    private Long id;
    private Long orgId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String message;
    private String status;
    private Long reviewerId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
