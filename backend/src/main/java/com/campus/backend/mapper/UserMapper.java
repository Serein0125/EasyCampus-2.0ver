package com.campus.backend.mapper;

import com.campus.backend.entity.User;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper {

    /**
     * 新增用户
     */
    @Insert("INSERT INTO users (username, password_hash, phone, email, nickname, avatar, gender, " +
            "school, major, grade, wechat, qq, bio, is_student, status, role) " +
            "VALUES (#{username}, #{passwordHash}, #{phone}, #{email}, #{nickname}, #{avatar}, #{gender}, " +
            "#{school}, #{major}, #{grade}, #{wechat}, #{qq}, #{bio}, #{isStudent}, 1, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(Long id);

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM users WHERE phone = #{phone}")
    User selectByPhone(String phone);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(String email);

    /**
     * 更新密码
     */
    @Update("UPDATE users SET password_hash = #{passwordHash}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(User user);

    /**
     * 更新用户资料
     */
    @Update("UPDATE users SET nickname = #{nickname}, avatar = #{avatar}, gender = #{gender}, " +
            "school = #{school}, campus = #{campus}, major = #{major}, grade = #{grade}, " +
            "wechat = #{wechat}, qq = #{qq}, bio = #{bio}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateProfile(User user);

    /**
     * 更新最后登录时间
     */
    @Update("UPDATE users SET last_login_at = #{loginAt} WHERE id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("loginAt") LocalDateTime loginAt);

    /**
     * 更新用户状态
     */
    @Update("UPDATE users SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据学校名称查询用户ID列表
     */
    @Select("SELECT id FROM users WHERE school = #{school} AND status = 1")
    java.util.List<Long> selectIdsBySchool(String school);

    @Select("SELECT id, nickname, username, avatar FROM users WHERE status = 1 AND (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%') OR id = #{keyword}) ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    java.util.List<User> searchUsers(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM users WHERE status = 1 AND (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))")
    int searchUserCount(@Param("keyword") String keyword);

    @Select("<script>SELECT id, nickname, username, avatar FROM users WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    java.util.List<User> selectBasicInfoByIds(@Param("ids") java.util.List<Long> ids);
}
