package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Post {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String postType;

    private Long boardId;

    private Long groupBuyId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Boolean isPinned;

    private Boolean isEssence;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String tags;

    private String campusTag;

    private String contact;

    private String imageUrls;

    private String coverImage;

    private Boolean isAd;

    private Integer exposureBoost;
}
