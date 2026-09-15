package com.campus.backend.service;

import com.campus.backend.dto.AiProductDescriptionDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 能力服务：商品发布助手等
 */
public interface AiService {

    /**
     * 流式生成商品标题与描述
     * 将大模型返回的文本增量通过 SseEmitter 逐段推送给前端
     *
     * @param dto     请求参数（图片 + 标题提示）
     * @param emitter SSE 发射器，由 Controller 创建并返回给前端
     */
    void streamProductDescription(AiProductDescriptionDTO dto, SseEmitter emitter);
}
