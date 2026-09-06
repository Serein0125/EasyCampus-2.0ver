package com.campus.backend.service.impl;

import com.campus.backend.common.ErrorCode;
import com.campus.backend.dto.UserVO;
import com.campus.backend.entity.UserFollow;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.mapper.UserFollowMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public boolean toggleFollow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessException(ErrorCode.FOLLOW_SELF_ERROR);
        }
        boolean alreadyFollowing = userFollowMapper.exists(followerId, followeeId);
        if (alreadyFollowing) {
            userFollowMapper.delete(followerId, followeeId);
            log.info("取消关注: followerId={}, followeeId={}", followerId, followeeId);
            return false;
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            userFollowMapper.insert(follow);
            log.info("关注: followerId={}, followeeId={}", followerId, followeeId);
            return true;
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return userFollowMapper.exists(followerId, followeeId);
    }

    @Override
    public int getFollowingCount(Long userId) {
        return userFollowMapper.countFollowing(userId);
    }

    @Override
    public int getFollowerCount(Long userId) {
        return userFollowMapper.countFollowers(userId);
    }

    @Override
    public List<UserVO> getFollowingList(Long userId, int offset, int limit) {
        List<UserFollow> follows = userFollowMapper.selectFollowing(userId, offset, limit);
        return follows.stream()
                .map(f -> toUserVO(f.getFolloweeId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserVO> getFollowerList(Long userId, int offset, int limit) {
        List<UserFollow> followers = userFollowMapper.selectFollowers(userId, offset, limit);
        return followers.stream()
                .map(f -> toUserVO(f.getFollowerId()))
                .collect(Collectors.toList());
    }

    private UserVO toUserVO(Long userId) {
        var user = userMapper.selectById(userId);
        if (user == null) return null;
        var vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setSchool(user.getSchool());
        return vo;
    }
}
