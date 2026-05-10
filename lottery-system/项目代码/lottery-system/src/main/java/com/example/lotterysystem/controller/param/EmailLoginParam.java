package com.example.lotterysystem.controller.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailLoginParam extends UserLoginParam {

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空！")
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空！")
    private String verificationCode;

}