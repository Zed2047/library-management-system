package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_book")
public class Book {

    @TableId(type=IdType.AUTO)
    private Long id;
    private String title;
    private String author;
    private String category;
    private Integer quantity;
    private int status;
}
