package com.example.lotterysystem.controller.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfoResult implements Serializable {
    private Long userId;
    private String userName;
    private String email;
    private String phoneNumber;
    private String identity;
}