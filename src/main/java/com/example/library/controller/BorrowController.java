package com.example.library.controller;

import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.entity.User;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/borrow")
public class BorrowController {
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookService bookService;

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String keyword,
                       HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        List<Borrow> borrows;

        if ("admin".equals(user.getRole())) {
            borrows = borrowService.list(); // admin看全部
        } else {
            borrows = borrowService.lambdaQuery()
                    .eq(Borrow::getUserId, user.getId())
                    .orderByDesc(Borrow::getBorrowTime)
                    .list();
        }

        // 填充用户名和图书名
        for (Borrow b : borrows) {
            User borrower = userService.getById(b.getUserId());
            if (borrower != null) {
                b.setUsername(borrower.getUsername());
            }
            Book book = bookService.getById(b.getBookId());
            if (book != null) {
                b.setBookTitle(book.getTitle());
            }
        }

        // 搜索过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            borrows = borrows.stream()
                    .filter(b -> (b.getUsername() != null && b.getUsername().contains(kw))
                            || (b.getBookTitle() != null && b.getBookTitle().contains(kw)))
                    .collect(Collectors.toList());
            model.addAttribute("keyword", kw);
        }

        // 逾期/临期标记
        LocalDateTime now = LocalDateTime.now();
        for (Borrow b : borrows) {
            if (b.getStatus() == 0) {
                if (now.isAfter(b.getDueTime())) {
                    b.setOverdue(true);
                } else if (now.plusDays(3).isAfter(b.getDueTime())) {
                    b.setDueSoon(true);
                }
            }
        }

        model.addAttribute("borrows", borrows);
        return "borrow-list";
    }
}
