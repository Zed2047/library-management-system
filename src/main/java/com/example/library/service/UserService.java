package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);
    boolean isUsernameTaken(String username);
    boolean changePassword(Long userId, String oldPassword, String newPassword);
}
