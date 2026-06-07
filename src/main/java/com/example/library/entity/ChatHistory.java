package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_chat_history")
public class ChatHistory {

    @TableId(type= IdType.AUTO)
    private Long id;
    private Long userId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
