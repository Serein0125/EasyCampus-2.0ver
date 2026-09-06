package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.CommentCreateDTO;
import com.campus.backend.dto.PostCommentVO;
import com.campus.backend.entity.Product;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.service.NotificationService;
import com.campus.backend.service.ProductCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v2/products")
@RequiredArgsConstructor
public class ProductCommentController {

    private final ProductCommentService productCommentService;
    private final NotificationService notificationService;
    private final ProductMapper productMapper;

    @PostMapping("/{productId}/comments")
    public Result<PostCommentVO> addComment(@PathVariable Long productId, @Valid @RequestBody CommentCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        PostCommentVO comment = productCommentService.addComment(productId, dto, userId);

        // 评论时通知商品卖家
        try {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                String content = "评论了你的商品「" + truncate(product.getName(), 20) + "」";
                notificationService.createNotification(product.getSellerId(), userId, "COMMENT", productId, "PRODUCT", content);
            }
        } catch (Exception e) {
            log.warn("发送评论通知失败: {}", e.getMessage());
        }

        return Result.success(comment);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        productCommentService.deleteComment(commentId, userId);
        return Result.success(null);
    }

    @GetMapping("/{productId}/comments")
    public Result<List<PostCommentVO>> getComments(@PathVariable Long productId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<PostCommentVO> comments = productCommentService.getComments(productId, userId, page, size);
        return Result.success(comments);
    }
}
