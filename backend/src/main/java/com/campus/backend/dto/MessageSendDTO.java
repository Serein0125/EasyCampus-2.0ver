package com.campus.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送消息请求DTO
 */
@Data
public class MessageSendDTO {

    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 关联的商品ID (可选，用于商品咨询场景) */
    private Long productId;

    /** 消息类型: text文本, image图片, system系统消息 (默认text) */
    private String messageType = "text";
}
