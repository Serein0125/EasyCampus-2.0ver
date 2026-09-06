package com.campus.backend.mapper;

import com.campus.backend.entity.OrgAuditLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrgAuditLogMapper {

    @Insert("INSERT INTO org_audit_logs (org_id, actor_id, action, target_id, detail) VALUES (#{orgId}, #{actorId}, #{action}, #{targetId}, #{detail})")
    int insert(OrgAuditLog log);

    @Select("SELECT * FROM org_audit_logs WHERE org_id = #{orgId} ORDER BY created_at DESC LIMIT #{limit}")
    List<OrgAuditLog> selectByOrgId(@Param("orgId") Long orgId, @Param("limit") int limit);
}
