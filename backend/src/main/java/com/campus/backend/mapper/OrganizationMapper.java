package com.campus.backend.mapper;

import com.campus.backend.entity.Organization;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrganizationMapper {

    @Insert("INSERT INTO trade_organizations (name, org_type, description, logo_url, banner_url, contact_email, website_url, location, founder_id, join_type, status) " +
            "VALUES (#{name}, #{orgType}, #{description}, #{logoUrl}, #{bannerUrl}, #{contactEmail}, #{websiteUrl}, #{location}, #{founderId}, #{joinType}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Organization org);

    @Select("SELECT * FROM trade_organizations WHERE id = #{id}")
    Organization selectById(Long id);

    @Select("SELECT * FROM trade_organizations WHERE status = 'APPROVED' ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<Organization> selectApproved(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM trade_organizations WHERE status = 'APPROVED'")
    int countApproved();

    @Select("SELECT * FROM trade_organizations WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT #{offset}, #{limit}")
    List<Organization> selectPending(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT o.* FROM trade_organizations o INNER JOIN org_members m ON o.id = m.org_id WHERE m.user_id = #{userId}")
    List<Organization> selectByUserId(Long userId);

    @Update("UPDATE trade_organizations SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE trade_organizations SET member_count = member_count + 1 WHERE id = #{id}")
    int incrementMemberCount(Long id);

    @Update("UPDATE trade_organizations SET member_count = GREATEST(0, member_count - 1) WHERE id = #{id}")
    int decrementMemberCount(Long id);

    @Select("SELECT * FROM trade_organizations WHERE name LIKE CONCAT('%', #{keyword}, '%') AND status = 'APPROVED' ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<Organization> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
}
