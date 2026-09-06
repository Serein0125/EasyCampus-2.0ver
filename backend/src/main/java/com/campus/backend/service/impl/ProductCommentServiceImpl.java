package com.campus.backend.service.impl;

import com.campus.backend.common.ErrorCode;
import com.campus.backend.dto.CommentCreateDTO;
import com.campus.backend.dto.PostCommentVO;
import com.campus.backend.entity.Product;
import com.campus.backend.entity.ProductComment;
import com.campus.backend.entity.User;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.ProductCommentMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.mapper.UserLikeMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.ProductCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommentServiceImpl implements ProductCommentService {

    private final ProductMapper productMapper;
    private final ProductCommentMapper productCommentMapper;
    private final UserMapper userMapper;
    private final UserLikeMapper userLikeMapper;

    @Override
    @Transactional
    public PostCommentVO addComment(Long productId, CommentCreateDTO dto, Long userId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NotFoundException("Product", productId);
        }
        if (dto.getParentId() != null) {
            ProductComment parentComment = productCommentMapper.selectById(dto.getParentId());
            if (parentComment == null) {
                throw new NotFoundException("Comment", dto.getParentId());
            }
        }
        ProductComment comment = new ProductComment();
        comment.setProductId(productId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        productCommentMapper.insert(comment);
        log.info("商品评论: productId={}, userId={}", productId, userId);
        return toCommentVO(comment, userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        ProductComment comment = productCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new NotFoundException("Comment", commentId);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_OWNERSHIP_ERROR);
        }
        productCommentMapper.deleteById(commentId);
        log.info("删除商品评论: commentId={}", commentId);
    }

    @Override
    public List<PostCommentVO> getComments(Long productId, Long currentUserId, int page, int size) {
        int offset = (page - 1) * size;
        List<ProductComment> topLevelComments = productCommentMapper.selectTopLevelByProductId(productId, offset, size);
        return topLevelComments.stream()
                .map(c -> toCommentVOWithReplies(c, currentUserId))
                .collect(Collectors.toList());
    }

    private PostCommentVO toCommentVO(ProductComment comment, Long currentUserId) {
        PostCommentVO vo = new PostCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getProductId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setStatus(comment.getStatus());
        vo.setCreatedAt(comment.getCreatedAt());

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            vo.setUserAvatar(user.getAvatar());
        }

        if (currentUserId != null) {
            vo.setIsLiked(userLikeMapper.exists(currentUserId, "COMMENT", comment.getId()));
        }

        return vo;
    }

    private PostCommentVO toCommentVOWithReplies(ProductComment comment, Long currentUserId) {
        PostCommentVO vo = toCommentVO(comment, currentUserId);
        List<ProductComment> replies = productCommentMapper.selectRepliesByParentId(comment.getId());
        if (replies != null && !replies.isEmpty()) {
            vo.setReplies(replies.stream()
                    .map(r -> toCommentVO(r, currentUserId))
                    .collect(Collectors.toList()));
        } else {
            vo.setReplies(new ArrayList<>());
        }
        return vo;
    }
}
