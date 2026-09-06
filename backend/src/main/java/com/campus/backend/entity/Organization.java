package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Organization {
    private Long id;
    private String name;
    private String orgType;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String contactEmail;
    private String websiteUrl;
    private String location;
    private Long founderId;
    private String joinType;
    private String status;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
