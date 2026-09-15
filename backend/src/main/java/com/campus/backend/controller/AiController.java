package com.campus.backend.controller;

import com.campus.backend.common.ErrorCode;
import com.campus.backend.dto.AiProductDescriptionDTO;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 助手控制器 - 商品发布文案生成
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/ai")
@Tag(name = "AI 助手", description = "商品发布 AI 文案生成")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /** AI 生成属于长耗时 IO 任务，放到独立线程池执行，避免阻塞请求线程 */
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool();

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Operation(summary = "AI 生成商品文案（SSE 流式）", description = "根据商品图片生成标题与描述，流式返回")
    @PostMapping(value = "/product/description", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateProductDescription(@RequestBody AiProductDescriptionDTO dto) {
        // 预校验：未配置 API Key 时直接返回 JSON 错误，避免进入流式后无法向用户清晰反馈
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "AI 服务未配置，请设置环境变量 DEEPSEEK_API_KEY");
        }
        SseEmitter emitter = new SseEmitter(120_000L);
        aiExecutor.execute(() -> aiService.streamProductDescription(dto, emitter));
        return emitter;
    }

    @PreDestroy
    public void shutdown() {
        aiExecutor.shutdown();
    }
}
