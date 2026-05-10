package com.example.lotterysystem.dao.mapper;

import com.example.lotterysystem.dao.dataobject.Encrypt;
import com.example.lotterysystem.dao.dataobject.UserDO;
import org.apache.ibatis.annotations.*;

import java.util.List;


@Mapper
public interface UserMapper {

    /**
     * 查询邮箱绑定的人数
     */
    @Select("select count(*) from user where email = #{email}")
    int countByMail(@Param("email") String email);

    @Select("select count(*) from user where phone_number = #{phoneNumber}")
    int countByPhone(@Param("phoneNumber") Encrypt phoneNumber);

    @Insert("insert into user (user_name, email, phone_number, password, identity)" +
            " values (#{userName}, #{email}, #{phoneNumber}, #{password}, #{identity})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(UserDO userDO);

    @Select("select * from user where email = #{email}")
    UserDO selectByMail(@Param("email") String email);

    @Select("select * from user where phone_number = #{phoneNumber}")
    UserDO selectByPhoneNumber(@Param("phoneNumber") Encrypt phoneNumber);

    /**
     * 根据ID查询用户
     */
    @Select("select * from user where id = #{id}")
    UserDO selectById(@Param("id") Long id);

    @Select("<script>" +
            " select * from user" +
            " <if test=\"identity!=null\">" +
            "    where identity = #{identity}" +
            " </if>" +
            " order by id desc" +
            " </script>")
    List<UserDO> selectUserListByIdentity(@Param("identity")String identity);

    @Select("<script>" +
            " select id from user" +
            " where id in" +
            " <foreach item='item' collection='items' open='(' separator=',' close=')'>" +
            " #{item}" +
            " </foreach>" +
            " </script>")
    List<Long> selectExistByIds(@Param("items") List<Long> ids);

    @Select("<script>" +
            " select * from user" +
            " where id in" +
            " <foreach item='item' collection='items' open='(' separator=',' close=')'>" +
            " #{item}" +
            " </foreach>" +
            " </script>")
    List<UserDO> batchSelectByIds(@Param("items") List<Long> ids);

    // ========== 删除方法 ==========

    /**
     * 根据ID删除用户
     */
    @Delete("DELETE FROM user WHERE id = #{userId}")
    int deleteById(@Param("userId") Long userId);

    /**
     * 根据ID批量删除用户
     */
    @Delete("<script>" +
            "DELETE FROM user WHERE id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByIds(@Param("userIds") List<Long> userIds);

    // ========== 分页查询方法 ==========

    /**
     * 分页查询用户列表
     */
    @Select("<script>" +
            "select * from user" +
            "<if test='identity != null'> where identity = #{identity}</if>" +
            " order by id desc limit #{offset}, #{size}" +
            "</script>")
    List<UserDO> selectUserPage(@Param("identity") String identity,
                                @Param("offset") int offset,
                                @Param("size") int size);

    /**
     * 统计用户数量
     */
    @Select("<script>" +
            "select count(*) from user" +
            "<if test='identity != null'> where identity = #{identity}</if>" +
            "</script>")
    int countUsers(@Param("identity") String identity);
}