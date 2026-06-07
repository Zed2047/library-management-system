package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_borrow")
public class Borrow {

    @TableId(type= IdType.AUTO)
    private Long id;

    private Long userId;
    private Long bookId;

    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    private int status;

    @TableField(exist=false)
    private String username;    // 借阅者姓名
    @TableField(exist=false)
    private String bookTitle;   // 图书名称
    @TableField(exist=false)
    private boolean overdue;
    @TableField(exist=false)
    private boolean dueSoon;
}