package com.example.lotterysystem.service;

import com.example.lotterysystem.entity.User;

public interface UserService {

    boolean register(User user);

    User loginByPhone(String phone, String password);

    User loginByEmail(String email, String password);

    boolean isPhoneExist(String phone);

    boolean isEmailExist(String email);

    User findById(Integer id);

    User findByEmail(String email);
}