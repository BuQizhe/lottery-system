package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    // 插入用户（注册）- 密码已加密存储
    @Insert("INSERT INTO user(name, phone, email, password, role) VALUES(#{name}, #{phone}, #{email}, #{password}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    // 根据手机号查询
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(String phone);

    // 根据邮箱查询
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);

    // 根据ID查询
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);

    // 查询所有用户（管理员用）
    @Select("SELECT * FROM user ORDER BY create_time DESC")
    List<User> findAll();

    // 更新用户信息
    @Update("UPDATE user SET name = #{name}, phone = #{phone}, email = #{email}, role = #{role} WHERE id = #{id}")
    int update(User user);

    // 删除用户
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Integer id);

    // 统计用户数量
    @Select("SELECT COUNT(*) FROM user")
    int count();
}