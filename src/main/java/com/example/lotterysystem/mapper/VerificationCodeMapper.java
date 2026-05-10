package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.VerificationCode;
import org.apache.ibatis.annotations.*;

@Mapper
public interface VerificationCodeMapper {

    @Insert("INSERT INTO verification_code(email, code, type, expire_time, used) VALUES(#{email}, #{code}, #{type}, #{expireTime}, #{used})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VerificationCode code);

    @Select("SELECT * FROM verification_code WHERE email = #{email} AND code = #{code} AND type = #{type} AND used = 0 AND expire_time > NOW() ORDER BY id DESC LIMIT 1")
    VerificationCode findValidCode(@Param("email") String email, @Param("code") String code, @Param("type") String type);

    @Update("UPDATE verification_code SET used = 1 WHERE id = #{id}")
    int markUsed(Integer id);
}