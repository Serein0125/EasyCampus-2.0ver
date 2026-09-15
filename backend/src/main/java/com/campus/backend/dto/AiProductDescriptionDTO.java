package com.campus.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 生成商品文案请求 DTO
 * 用于「商品发布助手」：根据已上传的商品图片 + 可选的标题提示，生成标题与描述
 */
@Data
public class AiProductDescriptionDTO {

    /** 商品图片 URL 列表（取第一张作为识别依据，如 "/uploads/2026/09/14/uuid.jpg"） */
    private List<String> imageUrls;

    /** 用户已填写的标题片段（可选，作为生成的参考） */
    private String titleHint;
}
