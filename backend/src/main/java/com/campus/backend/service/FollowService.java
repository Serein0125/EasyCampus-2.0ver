package com.campus.backend.service;

import com.campus.backend.dto.UserVO;
import java.util.List;

public interface FollowService {

    boolean toggleFollow(Long followerId, Long followeeId);

    boolean isFollowing(Long followerId, Long followeeId);

    int getFollowingCount(Long userId);

    int getFollowerCount(Long userId);

    List<UserVO> getFollowingList(Long userId, int offset, int limit);

    List<UserVO> getFollowerList(Long userId, int offset, int limit);
}
