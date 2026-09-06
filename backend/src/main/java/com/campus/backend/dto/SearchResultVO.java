package com.campus.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SearchResultVO {

    private List<PostItem> posts;
    private int postTotal;
    private List<ProductItem> products;
    private int productTotal;
    private List<UserItem> users;
    private int userTotal;
    private String keyword;
    private int page;
    private int size;

    @Data
    public static class PostItem {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private String title;
        private String content;
        private String postType;
        private String postTypeText;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private String tags;
        private LocalDateTime createdAt;
        private String matchedField;
    }

    @Data
    public static class ProductItem {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private String coverImage;
        private List<String> imageUrls;
        private Long sellerId;
        private String sellerName;
        private String sellerAvatar;
        private Integer conditionLevel;
        private String conditionText;
        private Integer status;
        private String statusText;
        private Integer viewCount;
        private Integer likeCount;
        private String location;
        private LocalDateTime createdAt;
        private String matchedField;
    }

    @Data
    public static class UserItem {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String school;
        private String major;
        private String bio;
        private String matchedField;
    }
}
