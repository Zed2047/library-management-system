package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.Book;

import java.util.List;

public interface BookService extends IService<Book> {
    List<Book> search(String keyword);
}
