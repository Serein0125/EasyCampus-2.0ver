package com.campus.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostCommentVO {

    private Long id;

    private Long postId;

    private Long userId;

    private String userName;

    private String userAvatar;

    private Long parentId;

    private String content;

    private Integer likeCount;

    private Boolean isLiked;

    private String status;

    private LocalDateTime createdAt;

    private List<PostCommentVO> replies;
}
