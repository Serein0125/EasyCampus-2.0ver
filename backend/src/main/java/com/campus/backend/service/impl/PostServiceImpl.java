package com.campus.backend.service.impl;

import com.campus.backend.common.ErrorCode;
import com.campus.backend.dto.CommentCreateDTO;
import com.campus.backend.dto.PostCommentVO;
import com.campus.backend.dto.PostCreateDTO;
import com.campus.backend.dto.PostQueryDTO;
import com.campus.backend.dto.PostVO;
import com.campus.backend.entity.Post;
import com.campus.backend.entity.PostComment;
import com.campus.backend.entity.User;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.CategoryMapper;
import com.campus.backend.mapper.PostCommentMapper;
import com.campus.backend.mapper.PostMapper;
import com.campus.backend.mapper.UserLikeMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostCommentMapper postCommentMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final UserLikeMapper userLikeMapper;

    @Override
    @Transactional
    public PostVO createPost(PostCreateDTO dto, Long userId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setPostType(dto.getPostType() != null ? dto.getPostType() : "DISCUSSION");
        post.setBoardId(dto.getBoardId());
        post.setTags(dto.getTags());
        post.setStartTime(dto.getStartTime());
        post.setEndTime(dto.getEndTime());
        post.setLocation(dto.getLocation());
        post.setContact(dto.getContact());
        // 处理图片
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            try {
                post.setImageUrls(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dto.getImageUrls()));
            } catch (Exception e) {
                log.warn("图片URL序列化失败", e);
            }
        }
        post.setCoverImage(dto.getCoverImage());
        postMapper.insert(post);
        log.info("发布帖子: id={}, userId={}, title={}", post.getId(), userId, dto.getTitle());
        return toPostVO(post, userId);
    }

    @Override
    @Transactional
    public PostVO updatePost(Long postId, PostCreateDTO dto, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new NotFoundException("Post", postId);
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_OWNERSHIP_ERROR);
        }
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        if (dto.getPostType() != null) {
            post.setPostType(dto.getPostType());
        }
        if (dto.getBoardId() != null) {
            post.setBoardId(dto.getBoardId());
        }
        post.setStartTime(dto.getStartTime());
        post.setEndTime(dto.getEndTime());
        post.setLocation(dto.getLocation());
        post.setContact(dto.getContact());
        post.setTags(dto.getTags());
        // 更新图片
        if (dto.getImageUrls() != null) {
            try {
                post.setImageUrls(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dto.getImageUrls()));
            } catch (Exception e) {
                log.warn("图片URL序列化失败", e);
            }
        }
        if (dto.getCoverImage() != null) {
            post.setCoverImage(dto.getCoverImage());
        }
        postMapper.update(post);
        return toPostVO(postMapper.selectById(postId), userId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new NotFoundException("Post", postId);
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_OWNERSHIP_ERROR);
        }
        postMapper.deleteById(postId);
        log.info("删除帖子: id={}", postId);
    }

    @Override
    @Transactional
    public PostVO getPostDetail(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new NotFoundException("Post", postId);
        }
        postMapper.incrementViewCount(postId);
        return toPostVO(post, currentUserId);
    }

    @Override
    public List<PostVO> getPostList(PostQueryDTO query, Long currentUserId) {
        List<Post> posts = postMapper.selectPostList(query);
        return posts.stream()
                .map(p -> toPostVO(p, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    public int getPostCount(PostQueryDTO query) {
        return postMapper.selectPostCount(query);
    }

    @Override
    public List<PostVO> getUserPosts(Long userId) {
        List<Post> posts = postMapper.selectByUserId(userId);
        return posts.stream()
                .map(p -> toPostVO(p, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PostCommentVO addComment(Long postId, CommentCreateDTO dto, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new NotFoundException("Post", postId);
        }
        if (dto.getParentId() != null) {
            PostComment parentComment = postCommentMapper.selectById(dto.getParentId());
            if (parentComment == null) {
                throw new NotFoundException("Comment", dto.getParentId());
            }
        }
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        postCommentMapper.insert(comment);
        postMapper.incrementCommentCount(postId);
        log.info("发布评论: postId={}, userId={}", postId, userId);
        return toCommentVO(comment, userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new NotFoundException("Comment", commentId);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_OWNERSHIP_ERROR);
        }
        postCommentMapper.deleteById(commentId);
        postMapper.decrementCommentCount(comment.getPostId());
        log.info("删除评论: commentId={}", commentId);
    }

    @Override
    public List<PostCommentVO> getComments(Long postId, Long currentUserId, int page, int size) {
        int offset = (page - 1) * size;
        List<PostComment> topLevelComments = postCommentMapper.selectTopLevelByPostId(postId, offset, size);
        return topLevelComments.stream()
                .map(c -> toCommentVOWithReplies(c, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void togglePin(Long postId, Long userId, Boolean isPinned) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new NotFoundException("Post", postId);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_OWNERSHIP_ERROR);
        }
        postMapper.togglePin(postId, isPinned);
    }

    @Override
    @Transactional
    public void toggleEssence(Long postId, Boolean isEssence) {
        postMapper.toggleEssence(postId, isEssence);
    }

    private PostVO toPostVO(Post post, Long currentUserId) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setPostType(post.getPostType());
        vo.setBoardId(post.getBoardId());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setIsPinned(post.getIsPinned());
        vo.setIsEssence(post.getIsEssence());
        vo.setStatus(post.getStatus());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setUpdatedAt(post.getUpdatedAt());
        vo.setStartTime(post.getStartTime());
        vo.setEndTime(post.getEndTime());
        vo.setLocation(post.getLocation());
        vo.setTags(post.getTags());
        vo.setContact(post.getContact());
        vo.setIsAd(post.getIsAd());
        vo.setExposureBoost(post.getExposureBoost());
        // 图片字段：JSON字符串 → List
        if (post.getImageUrls() != null && !post.getImageUrls().isBlank()) {
            try {
                vo.setImageUrls(new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    post.getImageUrls(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
            } catch (Exception e) {
                log.warn("图片URL反序列化失败", e);
            }
        }
        vo.setCoverImage(post.getCoverImage());

        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            vo.setUserAvatar(user.getAvatar());
        }

        if (post.getBoardId() != null) {
            var category = categoryMapper.findById(post.getBoardId());
            if (category != null) {
                vo.setBoardName(category.getName());
            }
        }

        if (currentUserId != null) {
            vo.setIsLiked(userLikeMapper.exists(currentUserId, "POST", post.getId()));
        }

        return vo;
    }

    private PostCommentVO toCommentVO(PostComment comment, Long currentUserId) {
        PostCommentVO vo = new PostCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
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

    private PostCommentVO toCommentVOWithReplies(PostComment comment, Long currentUserId) {
        PostCommentVO vo = toCommentVO(comment, currentUserId);
        List<PostComment> replies = postCommentMapper.selectRepliesByParentId(comment.getId());
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
