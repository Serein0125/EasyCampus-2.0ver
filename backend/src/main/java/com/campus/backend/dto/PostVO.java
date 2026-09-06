package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostVO {

    private Long id;

    private Long userId;

    private String userName;

    private String userAvatar;

    private String title;

    private String content;

    private String postType;

    private Long boardId;

    private String boardName;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Boolean isPinned;

    private Boolean isEssence;

    private Boolean isLiked;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String tags;

    private String contact;

    private List<String> imageUrls;

    private String coverImage;

    private Boolean isAd;

    private Integer exposureBoost;

    public String getPostTypeText() {
        if (postType == null) return "讨论";
        return switch (postType) {
            case "DISCUSSION" -> "讨论";
            case "SHOWCASE" -> "展示";
            case "HELP" -> "求助";
            case "ACTIVITY" -> "活动";
            default -> postType;
        };
    }
}
