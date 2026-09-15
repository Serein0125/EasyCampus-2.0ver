package com.campus.backend.service.impl;

import com.campus.backend.dto.AiProductDescriptionDTO;
import com.campus.backend.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 能力实现：调用 DeepSeek 多模态视觉模型（deepseek-v4-flash-vision-exp）
 * 将商品图片 + 提示词发送给大模型，并以 SSE 流式方式把增量文本转发给前端。
 *
 * 设计要点：
 * 1. 使用 JDK 内置 HttpClient（Java 17），无需额外依赖即可做流式读取
 * 2. 后端直连 DeepSeek，API Key 只存于服务端环境变量，不暴露给前端
 * 3. SseEmitter 把大模型逐 token 的增量透传给前端，实现打字机效果
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-v4-flash-vision-exp}")
    private String model;

    @Value("${app.upload.base-path:./uploads}")
    private String uploadBasePath;

    /** 单张 base64 图片上限（约 5MB 原图），超出则拒绝，避免请求体过大 */
    private static final int MAX_IMAGE_BASE64_LENGTH = 7_000_000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public AiServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void streamProductDescription(AiProductDescriptionDTO dto, SseEmitter emitter) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("AI 服务未配置 API Key，请设置环境变量 DEEPSEEK_API_KEY");
            }

            // 1. 读取商品图片并转为 base64（取第一张封面）
            String imageBase64 = null;
            String mime = "image/jpeg";
            if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
                String url = dto.getImageUrls().get(0);
                mime = detectMimeType(url);
                imageBase64 = readImageAsBase64(url);
                if (imageBase64.length() > MAX_IMAGE_BASE64_LENGTH) {
                    throw new IllegalStateException("图片过大，无法用于 AI 识别，请换一张更小的图片");
                }
            }

            // 2. 构造 OpenAI 兼容的请求体
            Map<String, Object> requestBody = buildRequestBody(dto, imageBase64, mime);

            // 3. 调用 DeepSeek 流式接口
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("AI 服务调用失败(" + response.statusCode() + "): " + errBody);
            }

            // 4. 逐行解析 SSE 流，把文本增量转发给前端
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || !line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    String chunk = extractDeltaContent(payload);
                    if (chunk != null && !chunk.isBlank()) {
                        emitter.send(SseEmitter.event().data(chunk));
                    }
                }
            }
            emitter.complete();
        } catch (Exception e) {
            log.error("AI 生成商品文案失败", e);
            try {
                String msg = e.getMessage() == null ? "生成失败，请稍后重试" : e.getMessage();
                if (msg.length() > 200) {
                    msg = msg.substring(0, 200);
                }
                // 用 [ERROR] 前缀标记错误，前端据此区分正常增量文本与错误提示
                emitter.send(SseEmitter.event().data("[ERROR]" + msg));
            } catch (Exception ignored) {
                // 前端可能已断开连接
            }
            emitter.complete();
        }
    }

    /**
     * 构造 DeepSeek 请求体（OpenAI Chat Completions 兼容格式，图文混合输入）
     */
    private Map<String, Object> buildRequestBody(AiProductDescriptionDTO dto, String imageBase64, String mime) {
        List<Map<String, Object>> content = new ArrayList<>();

        if (imageBase64 != null) {
            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", Map.of("url", "data:" + mime + ";base64," + imageBase64));
            content.add(imagePart);
        }

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", buildPrompt(dto.getTitleHint()));
        content.add(textPart);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", true);
        // 注意：该模型会先流式输出大量 reasoning_content（思维过程）再输出正文，
        // max_tokens 若设太小（如 600）会被思考阶段耗尽，导致正文始终为空、前端收不到文案
        body.put("max_tokens", 3000);
        body.put("messages", List.of(message));
        return body;
    }

    /**
     * 提示词：约束大模型输出「标题 + 描述」的固定格式，便于前端解析
     */
    private String buildPrompt(String titleHint) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是校园二手交易平台的商品文案助手。请根据用户上传的商品图片识别商品，并生成发布文案。\n\n");
        sb.append("请严格按照以下格式输出，不要输出任何额外内容或解释：\n\n");
        sb.append("标题：<15字以内的商品标题>\n");
        sb.append("描述：<80-120字的营销文案，包含商品成色、核心卖点、适合人群>\n\n");
        sb.append("要求：标题简洁吸引人且不夸张；描述口语化、真诚，符合校园二手交易场景；若无法清晰识别，基于常识合理推测。");
        if (titleHint != null && !titleHint.isBlank()) {
            sb.append("\n\n用户已填写的标题参考：").append(titleHint.trim()).append("，可在此基础上优化。");
        }
        return sb.toString();
    }

    /**
     * 解析 SSE 数据行中的增量文本（choices[0].delta.content）
     * 该模型的思考过程走 reasoning_content 字段，content 在思考阶段为 JSON null，
     * 此时必须返回 null（注意：NullNode.asText() 会得到字符串 "null"，必须显式判空过滤），
     * 避免把无意义的 "null" 推给前端
     */
    private String extractDeltaContent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode content = choices.get(0).path("delta").path("content");
                if (content.isMissingNode() || content.isNull()) {
                    return null;
                }
                return content.asText();
            }
        } catch (Exception e) {
            log.warn("解析 SSE 数据失败: {}", payload, e);
        }
        return null;
    }

    /**
     * 根据图片 URL 读取本地文件并转为 base64
     * URL 形如 "/uploads/2026/09/14/uuid.jpg" → 本地 "uploads/2026/09/14/uuid.jpg"
     */
    private String readImageAsBase64(String imageUrl) throws Exception {
        String relative = imageUrl;
        if (relative.startsWith("/uploads")) {
            relative = relative.substring("/uploads".length());
        }
        relative = relative.replaceFirst("^/+", "");

        Path base = Paths.get(uploadBasePath);
        if (!base.isAbsolute()) {
            base = base.toAbsolutePath();
        }
        Path file = base.resolve(relative).normalize();

        byte[] bytes = Files.readAllBytes(file);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 根据图片 URL 后缀判断 MIME 类型
     */
    private String detectMimeType(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
