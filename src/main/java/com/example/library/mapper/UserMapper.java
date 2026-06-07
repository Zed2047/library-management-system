package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE username = #{username} AND password =#{password} AND status = 1 LIMIT 1")
    User login(String username, String password);

    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username}")
    long countByUsername(String username);
}
