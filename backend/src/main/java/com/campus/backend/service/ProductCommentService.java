package com.campus.backend.service;

import com.campus.backend.dto.CommentCreateDTO;
import com.campus.backend.dto.PostCommentVO;
import java.util.List;

public interface ProductCommentService {

    PostCommentVO addComment(Long productId, CommentCreateDTO dto, Long userId);

    void deleteComment(Long commentId, Long userId);

    List<PostCommentVO> getComments(Long productId, Long currentUserId, int page, int size);
}
