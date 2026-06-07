package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_review")
public class Review {
    @TableId(type= IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private String content;
    private int rating;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String username;
}
