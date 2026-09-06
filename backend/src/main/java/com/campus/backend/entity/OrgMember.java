package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrgMember {
    private Long id;
    private Long orgId;
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;
}
