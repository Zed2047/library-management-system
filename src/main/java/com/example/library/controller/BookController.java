package com.example.library.controller;

import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.entity.Review;
import com.example.library.entity.User;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.ReviewService;
import com.example.library.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/book")
public class BookController {
    @Autowired
    private BookService bookService;
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String keyword, Model model) {
        List<Book> books;
        if (keyword != null && !keyword.trim().isEmpty()) {
            books = bookService.search(keyword.trim());
            model.addAttribute("keyword", keyword);
        } else {
            books = bookService.list();
        }
        model.addAttribute("books", books);
        return "book-list";
    }
    @GetMapping("/detail")
    public String detail(@RequestParam Long id, Model model, HttpSession session) {
        Book book = bookService.getById(id);
        model.addAttribute("book", book);

        User user = (User) session.getAttribute("user");
        Borrow myBorrow = borrowService.lambdaQuery()
                .eq(Borrow::getUserId, user.getId())
                .eq(Borrow::getBookId, id)
                .eq(Borrow::getStatus, 0)
                .one();
        model.addAttribute("myBorrow", myBorrow);

        List<Review> review = reviewService.lambdaQuery()
                .eq(Review::getBookId, id)
                .orderByDesc(Review::getCreateTime)
                .list();
        model.addAttribute("reviews",review);

        int totalRating = 0;
        for (Review r : review) {
            User u = userService.getById(r.getUserId());
            if (u != null) {
                r.setUsername(u.getUsername());
            }
            totalRating += r.getRating();
        }
        double avgRating = review.isEmpty() ? 0 : (double) totalRating / review.size();
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("avgRatingStr", String.format("%.1f", avgRating));
        model.addAttribute("reviewCount", review.size());

        return "book-detail";
    }
    @PostMapping("/borrow")
    public String borrow(@RequestParam Long bookId, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");

        // 借阅记录
        Borrow borrow = new Borrow();
        borrow.setUserId(user.getId());
        borrow.setBookId(bookId);
        borrow.setBorrowTime(LocalDateTime.now());
        // dueTime：借阅后 30 天到期
        borrow.setDueTime(LocalDateTime.now().plusDays(30));
        borrow.setStatus(0);  // 0 = 借出中

        Book book = bookService.getById(bookId);
        if (book == null || book.getQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("error", "该书不存在或已被借完");
            return "redirect:/book/list";
        }

        borrowService.save(borrow);
        book.setQuantity(book.getQuantity() - 1);
        book.setStatus(book.getQuantity() > 0 ? 1 : 0);
        bookService.updateById(book);

        return "redirect:/borrow/list";
    }
    @GetMapping("/toAdd")
    public String toAdd(HttpSession session){
        User user = (User) session.getAttribute("user");
        if (!"admin".equals(user.getRole())) {
            return "redirect:/book/list";
        }
        return "book-add";
    }
    @PostMapping("/add")
    public String add(Book book, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (!"admin".equals(user.getRole())) {
            return "redirect:/book/list";
        }
        book.setStatus(book.getQuantity() != null && book.getQuantity() > 0 ? 1 : 0);
        bookService.save(book);
        return "redirect:/book/list";
    }
    @GetMapping("/toEdit")
    public String toEdit(@RequestParam Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (!"admin".equals(user.getRole())) {
            return "redirect:/book/list";
        }
        Book book = bookService.getById(id);
        model.addAttribute("book", book);
        return "book-edit";
    }
    @PostMapping("/doEdit")
    public String doEdit(Book book, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (!"admin".equals(user.getRole())) {
            return "redirect:/book/list";
        }
        bookService.updateById(book);
        return "redirect:/book/list";
    }
    @GetMapping("/delete")
    public String delete(@RequestParam Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (!"admin".equals(user.getRole())) {
            return "redirect:/book/list";
        }
        long activeCount = borrowService.lambdaQuery()
                .eq(Borrow::getBookId, id)
                .eq(Borrow::getStatus, 0)
                .count();
        if (activeCount > 0) {
            redirectAttributes.addFlashAttribute("error", "该书有未归还的借阅记录，无法删除");
            return "redirect:/book/list";
        }
        bookService.removeById(id);
        return "redirect:/book/list";
    }

    @PostMapping("/return")
    public String returnBook(@RequestParam Long borrowId, HttpSession session) {
        Borrow borrow = borrowService.getById(borrowId);
        if (borrow == null || borrow.getStatus() != 0) {
            return "redirect:/borrow/list";
        }

        User user = (User) session.getAttribute("user");
        if (!"admin".equals(user.getRole()) && !borrow.getUserId().equals(user.getId())) {
            return "redirect:/borrow/list";
        }

        borrow.setReturnTime(LocalDateTime.now());
        borrow.setStatus(1);
        borrowService.updateById(borrow);

        Book book = bookService.getById(borrow.getBookId());
        book.setQuantity(book.getQuantity() + 1);
        book.setStatus(book.getQuantity() > 0 ? 1 : 0);
        bookService.updateById(book);

        return "redirect:/borrow/list";
    }
}
