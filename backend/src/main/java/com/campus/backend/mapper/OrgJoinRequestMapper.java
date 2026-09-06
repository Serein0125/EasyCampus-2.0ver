package com.campus.backend.mapper;

import com.campus.backend.entity.OrgJoinRequest;
import com.campus.backend.dto.JoinRequestVO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrgJoinRequestMapper {

    @Insert("INSERT INTO org_join_requests (org_id, user_id, message) VALUES (#{orgId}, #{userId}, #{message})")
    int insert(OrgJoinRequest req);

    @Update("UPDATE org_join_requests SET status = #{status}, reviewer_id = #{reviewerId}, reviewed_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("reviewerId") Long reviewerId);

    @Select("SELECT r.id, r.org_id, r.user_id, COALESCE(u.nickname, u.username) as user_name, u.avatar as user_avatar, " +
            "r.message, r.status, r.reviewer_id, r.created_at, r.reviewed_at " +
            "FROM org_join_requests r LEFT JOIN users u ON r.user_id = u.id " +
            "WHERE r.org_id = #{orgId} AND r.status = 'PENDING' ORDER BY r.created_at ASC")
    List<JoinRequestVO> selectPendingByOrgId(Long orgId);

    @Select("SELECT * FROM org_join_requests WHERE org_id = #{orgId} AND user_id = #{userId} AND status = 'PENDING'")
    OrgJoinRequest selectPendingByOrgAndUser(@Param("orgId") Long orgId, @Param("userId") Long userId);

    @Select("SELECT * FROM org_join_requests WHERE id = #{id}")
    OrgJoinRequest selectById(Long id);
}
