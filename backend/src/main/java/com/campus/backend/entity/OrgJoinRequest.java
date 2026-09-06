package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrgJoinRequest {
    private Long id;
    private Long orgId;
    private Long userId;
    private String message;
    private String status;
    private Long reviewerId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
