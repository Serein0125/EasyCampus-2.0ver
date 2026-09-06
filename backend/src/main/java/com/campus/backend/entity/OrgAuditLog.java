package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrgAuditLog {
    private Long id;
    private Long orgId;
    private Long actorId;
    private String action;
    private Long targetId;
    private String detail;
    private LocalDateTime createdAt;
}
