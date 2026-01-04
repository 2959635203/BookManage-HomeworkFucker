package com.northgod.server.controller;

import com.northgod.server.entity.Book;
import com.northgod.server.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/books")
@Validated
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBooks(
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {

        try {
            logger.debug("获取书籍列表，页码: {}, 大小: {}, 排序: {}", page, size, sortBy);
            Page<Book> bookPage = bookService.getAllBooks(page, size, sortBy, direction);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bookPage.getContent());
            response.put("pagination", Map.of(
                    "currentPage", bookPage.getNumber(),
                    "pageSize", bookPage.getSize(),
                    "totalItems", bookPage.getTotalElements(),
                    "totalPages", bookPage.getTotalPages(),
                    "first", bookPage.isFirst(),
                    "last", bookPage.isLast()
            ));
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                    .body(response);
        } catch (Exception e) {
            logger.error("获取书籍列表失败", e);
            return createErrorResponse("获取书籍列表失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBookById(@PathVariable("id") Long id) {
        try {
            logger.debug("获取书籍详情，ID: {}", id);
            return bookService.getBookById(id)
                    .map(book -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", book);
                        return ResponseEntity.ok()
                                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS))
                                .eTag(String.valueOf(book.getVersion()))
                                .body(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "书籍不存在");
                        errorResponse.put("code", "BOOK_NOT_FOUND");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(errorResponse);
                    });
        } catch (Exception e) {
            logger.error("获取书籍详情失败，ID: {}", id, e);
            return createErrorResponse("获取书籍详情失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchBooks(
            @RequestParam(value = "keyword", required = true) String keyword,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        try {
            logger.debug("搜索书籍，关键字: {}, 页码: {}, 大小: {}", keyword, page, size);
            Page<Book> bookPage = bookService.searchBooks(keyword, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bookPage.getContent());
            response.put("pagination", Map.of(
                    "currentPage", bookPage.getNumber(),
                    "pageSize", bookPage.getSize(),
                    "totalItems", bookPage.getTotalElements(),
                    "totalPages", bookPage.getTotalPages()
            ));
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                    .body(response);
        } catch (Exception e) {
            logger.error("搜索书籍失败，关键字: {}", keyword, e);
            return createErrorResponse("搜索书籍失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search/fast")
    public ResponseEntity<Map<String, Object>> searchBooksFast(@RequestParam(value = "keyword", required = true) String keyword) {
        try {
            logger.debug("快速搜索书籍，关键字: {}", keyword);
            List<Book> books = bookService.searchBooksFast(keyword);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", books);
            response.put("total", books.size());
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                    .body(response);
        } catch (Exception e) {
            logger.error("快速搜索书籍失败，关键字: {}", keyword, e);
            return createErrorResponse("快速搜索书籍失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createBook(@Valid @RequestBody Book book) {
        try {
            logger.debug("创建书籍，ISBN: {}, 标题: {}", book.getIsbn(), book.getTitle());
            Book savedBook = bookService.saveBook(book);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "书籍创建成功");
            response.put("data", savedBook);
            response.put("bookId", savedBook.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("创建书籍失败，ISBN: {}", book.getIsbn(), e);
            return createErrorResponse("创建书籍失败: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> createBooksBatch(@Valid @RequestBody List<Book> books) {
        try {
            logger.debug("批量创建书籍，数量: {}", books.size());
            if (books.size() > 100) {
                return createErrorResponse("单次批量创建不能超过100条记录",
                        HttpStatus.BAD_REQUEST);
            }
            List<Book> savedBooks = bookService.saveAllBooks(books);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("批量创建成功，共 %d 本书籍", savedBooks.size()));
            response.put("data", savedBooks);
            response.put("count", savedBooks.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("批量创建书籍失败", e);
            return createErrorResponse("批量创建书籍失败: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBook(
            @PathVariable("id") Long id,
            @Valid @RequestBody Book book) {
        try {
            logger.debug("更新书籍，ID: {}", id);
            book.setId(id);
            Book updatedBook = bookService.saveBook(book);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "书籍更新成功");
            response.put("data", updatedBook);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新书籍失败，ID: {}", id, e);
            return createErrorResponse("更新书籍失败: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBook(@PathVariable("id") Long id) {
        try {
            logger.debug("删除书籍，ID: {}", id);
            bookService.deleteBook(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "书籍删除成功");
            response.put("bookId", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除书籍失败，ID: {}", id, e);
            return createErrorResponse("删除书籍失败: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteBooksBatch(@RequestBody List<Long> ids) {
        try {
            logger.debug("批量删除书籍，数量: {}", ids.size());
            if (ids.size() > 100) {
                return createErrorResponse("单次批量删除不能超过100条记录",
                        HttpStatus.BAD_REQUEST);
            }
            int deletedCount = bookService.deleteBooksBatch(ids);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("批量删除成功，共 %d 本书籍", deletedCount));
            response.put("deletedCount", deletedCount);
            response.put("requestedCount", ids.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量删除书籍失败", e);
            return createErrorResponse("批量删除书籍失败: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockBooks() {
        try {
            logger.debug("获取低库存书籍");
            List<Book> lowStockBooks = bookService.findLowStockBooks();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", lowStockBooks);
            response.put("count", lowStockBooks.size());
            response.put("warning", !lowStockBooks.isEmpty() ? "有低库存书籍需要补货" : "库存充足");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                    .body(response);
        } catch (Exception e) {
            logger.error("获取低库存书籍失败", e);
            return createErrorResponse("获取低库存书籍失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBookStats() {
        try {
            logger.debug("获取书籍统计信息");
            Map<String, Object> stats = bookService.getStatistics();
            BigDecimal inventoryValue = bookService.calculateTotalInventoryValue();
            Runtime runtime = Runtime.getRuntime();
            stats.put("system", Map.of(
                    "memoryTotal", runtime.totalMemory(),
                    "memoryFree", runtime.freeMemory(),
                    "memoryUsed", runtime.totalMemory() - runtime.freeMemory(),
                    "availableProcessors", runtime.availableProcessors()
            ));
            stats.put("inventoryValue", inventoryValue);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS))
                    .body(response);
        } catch (Exception e) {
            logger.error("获取书籍统计信息失败", e);
            return createErrorResponse("获取统计信息失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Map<String, Object>> getBookTransactions(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        try {
            logger.debug("获取书籍交易记录，书籍ID: {}", id);
            if (!bookService.getBookById(id).isPresent()) {
                return createErrorResponse("书籍不存在", HttpStatus.NOT_FOUND);
            }
            // 功能待实现
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "功能待实现");
            response.put("bookId", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取书籍交易记录失败，书籍ID: {}", id, e);
            return createErrorResponse("获取交易记录失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 获取回收站中的书籍（已删除的书籍）
     */
    @GetMapping("/deleted")
    public ResponseEntity<Map<String, Object>> getDeletedBooks(
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(value = "sortBy", defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        try {
            logger.debug("获取回收站书籍列表，页码: {}, 大小: {}", page, size);
            Page<Book> bookPage = bookService.getDeletedBooks(page, size, sortBy, direction);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bookPage.getContent());
            response.put("pagination", Map.of(
                    "currentPage", bookPage.getNumber(),
                    "pageSize", bookPage.getSize(),
                    "totalItems", bookPage.getTotalElements(),
                    "totalPages", bookPage.getTotalPages(),
                    "first", bookPage.isFirst(),
                    "last", bookPage.isLast()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取回收站书籍列表失败", e);
            return createErrorResponse("获取回收站书籍列表失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 恢复已删除的书籍
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, Object>> restoreBook(@PathVariable("id") Long id) {
        try {
            logger.debug("恢复书籍，ID: {}", id);
            bookService.restoreBook(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "书籍恢复成功");
            response.put("bookId", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("恢复书籍失败，ID: {}", id, e);
            return createErrorResponse("恢复书籍失败: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(response);
    }
}