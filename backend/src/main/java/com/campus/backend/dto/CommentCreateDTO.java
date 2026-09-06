package com.campus.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 2000, message = "评论长度不能超过2000个字符")
    private String content;

    private Long parentId;
}
