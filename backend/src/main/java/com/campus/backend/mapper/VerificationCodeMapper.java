package com.campus.backend.mapper;

import com.campus.backend.entity.VerificationCode;
import org.apache.ibatis.annotations.*;

/**
 * 验证码数据访问层
 */
@Mapper
public interface VerificationCodeMapper {

    /**
     * 插入验证码记录
     */
    @Insert("INSERT INTO verification_codes (account, code, type, used, expire_at, created_at) " +
            "VALUES (#{account}, #{code}, #{type}, 0, #{expireAt}, CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VerificationCode verificationCode);

    /**
     * 查询最新的未使用验证码
     * 按创建时间倒序取第一条
     */
    @Select("SELECT * FROM verification_codes " +
            "WHERE account = #{account} AND type = #{type} AND used = 0 AND expire_at > CURRENT_TIMESTAMP " +
            "ORDER BY created_at DESC LIMIT 1")
    VerificationCode selectLatestUnused(@Param("account") String account, @Param("type") String type);

    /**
     * 标记验证码为已使用
     */
    @Update("UPDATE verification_codes SET used = 1 WHERE id = #{id}")
    int markUsed(@Param("id") Long id);

    /**
     * 清理过期的验证码（定时任务可调用）
     */
    @Delete("DELETE FROM verification_codes WHERE expire_at < CURRENT_TIMESTAMP")
    int cleanExpired();
}
