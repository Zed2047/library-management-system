package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.entity.Borrow;
import com.example.library.mapper.BorrowMapper;
import com.example.library.service.BorrowService;
import org.springframework.stereotype.Service;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, Borrow> implements BorrowService {

}
