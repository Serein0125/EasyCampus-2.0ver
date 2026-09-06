package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MemberVO {
    private Long id;
    private Long orgId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String role;
    private LocalDateTime joinedAt;
}
