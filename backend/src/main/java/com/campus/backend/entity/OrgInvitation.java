package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrgInvitation {
    private Long id;
    private Long orgId;
    private Long inviterId;
    private Long inviteeId;
    private String inviteCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
