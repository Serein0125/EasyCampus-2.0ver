package com.campus.backend.mapper;

import com.campus.backend.entity.OrgInvitation;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrgInvitationMapper {

    @Insert("INSERT INTO org_invitations (org_id, inviter_id, invitee_id, invite_code) VALUES (#{orgId}, #{inviterId}, #{inviteeId}, #{inviteCode})")
    int insert(OrgInvitation inv);

    @Update("UPDATE org_invitations SET status = #{status}, responded_at = NOW() WHERE invite_code = #{inviteCode} AND status = 'PENDING'")
    int respondByCode(@Param("inviteCode") String inviteCode, @Param("status") String status);

    @Select("SELECT * FROM org_invitations WHERE invite_code = #{inviteCode}")
    OrgInvitation selectByCode(String inviteCode);

    @Select("SELECT * FROM org_invitations WHERE org_id = #{orgId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<OrgInvitation> selectByOrgId(@Param("orgId") Long orgId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM org_invitations WHERE invitee_id = #{userId} ORDER BY created_at DESC")
    List<OrgInvitation> selectByInviteeId(Long userId);

    @Select("SELECT * FROM org_invitations WHERE org_id = #{orgId} AND invitee_id = #{userId} AND status = 'PENDING' LIMIT 1")
    OrgInvitation selectPendingByOrgAndUser(@Param("orgId") Long orgId, @Param("userId") Long userId);
}
