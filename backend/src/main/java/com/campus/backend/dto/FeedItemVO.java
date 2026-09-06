package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FeedItemVO {

    private String itemType;

    private Long id;

    private String title;

    private String content;

    private String coverImage;

    private String price;

    private String originalPrice;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    private Boolean isLiked;

    private Long userId;

    private String userName;

    private String userAvatar;

    private Long categoryId;

    private String categoryName;

    private Long boardId;

    private String boardName;

    private String postType;

    private Boolean hasStory;

    private String conditionText;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime startTime;

    private Boolean isAd;

    private Integer exposureBoost;

    private String tags;
}
