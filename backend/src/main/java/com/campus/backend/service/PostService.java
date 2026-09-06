package com.campus.backend.service;

import com.campus.backend.dto.PostCreateDTO;
import com.campus.backend.dto.PostVO;
import com.campus.backend.dto.PostQueryDTO;
import com.campus.backend.dto.PostCommentVO;
import com.campus.backend.dto.CommentCreateDTO;
import java.util.List;

public interface PostService {

    PostVO createPost(PostCreateDTO dto, Long userId);

    PostVO updatePost(Long postId, PostCreateDTO dto, Long userId);

    void deletePost(Long postId, Long userId);

    PostVO getPostDetail(Long postId, Long currentUserId);

    List<PostVO> getPostList(PostQueryDTO query, Long currentUserId);

    int getPostCount(PostQueryDTO query);

    List<PostVO> getUserPosts(Long userId);

    PostCommentVO addComment(Long postId, CommentCreateDTO dto, Long userId);

    void deleteComment(Long commentId, Long userId);

    List<PostCommentVO> getComments(Long postId, Long currentUserId, int page, int size);

    void togglePin(Long postId, Long userId, Boolean isPinned);

    void toggleEssence(Long postId, Boolean isEssence);
}
