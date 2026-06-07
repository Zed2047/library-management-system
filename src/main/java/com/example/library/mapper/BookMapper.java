package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
    @Select("SELECT * FROM t_book WHERE "
            + "(title LIKE CONCAT('%', #{keyword}, '%') OR "
            + " author LIKE CONCAT('%', #{keyword}, '%') OR "
            + " category LIKE CONCAT('%', #{keyword}, '%')) "
            + "ORDER BY id DESC")
    List<Book> search(@Param("keyword") String keyword);
}
