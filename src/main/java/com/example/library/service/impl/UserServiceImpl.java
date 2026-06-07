package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.entity.User;
import com.example.library.mapper.UserMapper;
import com.example.library.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username,String password){
        return baseMapper.login(username,password);
    }
    @Override
    public boolean isUsernameTaken(String username) {
        return baseMapper.countByUsername(username) > 0;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword){
        User user = baseMapper.selectById(userId);
        if (user != null && user.getPassword().equals(oldPassword)) {
            user.setPassword(newPassword);
            return updateById(user);
        }
        return false;
    }
}
