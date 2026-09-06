package com.campus.backend.mapper;

import com.campus.backend.entity.OrgMember;
import com.campus.backend.dto.MemberVO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrgMemberMapper {

    @Insert("INSERT INTO org_members (org_id, user_id, role) VALUES (#{orgId}, #{userId}, #{role})")
    int insert(OrgMember member);

    @Delete("DELETE FROM org_members WHERE org_id = #{orgId} AND user_id = #{userId}")
    int delete(@Param("orgId") Long orgId, @Param("userId") Long userId);

    @Select("SELECT * FROM org_members WHERE org_id = #{orgId} AND user_id = #{userId} LIMIT 1")
    OrgMember selectByOrgAndUser(@Param("orgId") Long orgId, @Param("userId") Long userId);

    @Select("SELECT m.id, m.org_id, m.user_id, COALESCE(u.nickname, u.username) as user_name, u.avatar as user_avatar, m.role, m.joined_at " +
            "FROM org_members m LEFT JOIN users u ON m.user_id = u.id " +
            "WHERE m.org_id = #{orgId} ORDER BY m.joined_at ASC LIMIT #{offset}, #{limit}")
    List<MemberVO> selectByOrgId(@Param("orgId") Long orgId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM org_members WHERE org_id = #{orgId}")
    int countByOrgId(Long orgId);

    @Update("UPDATE org_members SET role = #{role} WHERE org_id = #{orgId} AND user_id = #{userId}")
    int updateRole(@Param("orgId") Long orgId, @Param("userId") Long userId, @Param("role") String role);
}
