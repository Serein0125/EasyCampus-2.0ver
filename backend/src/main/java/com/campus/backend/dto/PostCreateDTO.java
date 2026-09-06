package com.campus.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostCreateDTO {

    @NotBlank(message = "帖子标题不能为空")
    @Size(min = 2, max = 200, message = "标题长度为2-200个字符")
    private String title;

    @NotBlank(message = "帖子内容不能为空")
    @Size(min = 1, max = 10000, message = "内容长度不能超过10000个字符")
    private String content;

    private String postType;

    private Long boardId;

    private String tags;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String contact;

    private List<String> imageUrls;

    private String coverImage;
}
