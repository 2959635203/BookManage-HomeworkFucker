package com.northgod.server.service;

import com.northgod.server.entity.Book;
import com.northgod.server.exception.BusinessException;
import com.northgod.server.repository.BookRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);
    private final BookRepository bookRepository;
    private final CacheService cacheService;

    @PersistenceContext
    private EntityManager entityManager;

    public BookService(BookRepository bookRepository, CacheService cacheService) {
        this.bookRepository = bookRepository;
        this.cacheService = cacheService;
    }

    // 分页查询，提高性能（只返回活跃的书籍）
    public Page<Book> getAllBooks(int page, int size, String sortBy, String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        // 使用自定义查询只返回活跃的书籍
        return bookRepository.findByIsActiveTrue(pageable);
    }

    @Cacheable(value = "books", key = "#id", unless = "#result == null")
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Cacheable(value = "books", key = "#isbn", unless = "#result == null")
    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    // 搜索书籍
    public Page<Book> searchBooks(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepository.searchByKeyword(keyword, pageable);
    }

    // 快速搜索
    public List<Book> searchBooksFast(String keyword) {
        return bookRepository.searchBooksFast(keyword);
    }

    // 批量查询优化
    public List<Book> getBooksByIds(List<Long> ids) {
        return bookRepository.findAllById(ids);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Book saveBook(Book book) {
        validateBook(book);
        
        // 如果是更新操作（有ID），需要先从数据库加载实体以避免version字段问题
        if (book.getId() != null) {
            Book existingBook = bookRepository.findById(book.getId())
                    .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));
            
            // 复制字段到已存在的实体（保留version字段）
            copyBookFields(book, existingBook);
            Book savedBook = bookRepository.save(existingBook);
            
            // 清除缓存（更新操作）
            cacheService.evictBookCache(savedBook.getId());
            if (savedBook.getIsbn() != null) {
                // 清除ISBN缓存需要手动处理，因为CacheService没有按ISBN清除的方法
                cacheService.evictAllBookCache();
            } else {
                cacheService.evictAllBookCache();
            }
            
            return savedBook;
        }
        
        // 新建操作，直接保存
        Book savedBook = bookRepository.save(book);
        
        // 清除所有书籍缓存（新建操作，因为ID是新的）
        cacheService.evictAllBookCache();
        
        return savedBook;
    }
    
    /**
     * 复制Book字段（用于更新操作，保留version等字段）
     */
    private void copyBookFields(Book source, Book target) {
        target.setIsbn(source.getIsbn());
        target.setTitle(source.getTitle());
        target.setAuthor(source.getAuthor());
        target.setPublisher(source.getPublisher());
        target.setPurchasePrice(source.getPurchasePrice());
        target.setSellingPrice(source.getSellingPrice());
        target.setStockQuantity(source.getStockQuantity());
        target.setMinStock(source.getMinStock());
        target.setCategory(source.getCategory());
        target.setPublicationYear(source.getPublicationYear());
        target.setIsActive(source.getIsActive());
        target.setThumbnailUrl(source.getThumbnailUrl());
        target.setDescription(source.getDescription());
        // 注意：不复制id和version字段，这些应该保持不变
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));
        
        // 软删除：设置isActive为false，而不是物理删除
        // 这样可以保留历史交易记录，同时标记书籍为已删除状态
        book.setIsActive(false);
        bookRepository.save(book);
        
        // 手动清除缓存（避免SpEL表达式参数名称问题）
        cacheService.evictBookCache(id);
        cacheService.evictAllBookCache();
        
        logger.info("软删除书籍: ID={}, 书名={}", id, book.getTitle());
    }
    
    /**
     * 获取已删除的书籍列表（回收站）
     */
    public Page<Book> getDeletedBooks(int page, int size, String sortBy, String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByIsActiveFalse(pageable);
    }
    
    /**
     * 恢复已删除的书籍
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void restoreBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));
        
        if (book.getIsActive()) {
            throw new BusinessException("BOOK_ALREADY_ACTIVE", "书籍已经是激活状态，无需恢复");
        }
        
        book.setIsActive(true);
        bookRepository.save(book);
        
        // 清除缓存
        cacheService.evictBookCache(id);
        cacheService.evictAllBookCache();
        
        logger.info("恢复书籍: ID={}, 书名={}", id, book.getTitle());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Caching(
            evict = {
                    @CacheEvict(value = "books", allEntries = true)
            }
    )
    public int deleteBooksBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // 使用数据库批量软删除，性能更优
        int deletedCount = bookRepository.softDeleteBooksBatch(ids);
        logger.info("批量软删除书籍完成，删除数量: {}", deletedCount);
        return deletedCount;
    }

    // 异步更新库存，提高响应速度
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Book> updateStockAsync(Long bookId, Integer quantity) {
        try {
            Book updatedBook = updateStock(bookId, quantity);
            return CompletableFuture.completedFuture(updatedBook);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Book updateStock(Long bookId, Integer quantity) {
        Optional<Book> bookOptional = bookRepository.findById(bookId);
        if (bookOptional.isPresent()) {
            Book book = bookOptional.get();
            int newStock = book.getStockQuantity() + quantity;
            if (newStock < 0) {
                throw new RuntimeException("库存不足，当前库存：" + book.getStockQuantity());
            }
            book.setStockQuantity(newStock);

            // 批量更新时使用版本控制
            bookRepository.save(book);

            // 清除缓存
            cacheService.evictBookCache(bookId);

            return book;
        }
        throw new RuntimeException("书籍不存在");
    }

    // 批量保存优化
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Book> saveAllBooks(List<Book> books) {
        books.forEach(this::validateBook);
        List<Book> savedBooks = bookRepository.saveAll(books);
        // 清除所有书籍缓存
        cacheService.evictAllBookCache();
        return savedBooks;
    }

    public List<Book> findLowStockBooks() {
        return bookRepository.findLowStockBooks();
    }

    // 新增方法：获取书籍统计信息
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 基本统计：激活的书籍总数
        long activeCount = bookRepository.countActiveBooks();
        stats.put("activeBooks", activeCount);

        // 2. 总库存量
        Long totalStock = bookRepository.sumStockQuantity();
        stats.put("totalStock", totalStock != null ? totalStock : 0);

        // 3. 低库存书籍数量
        List<Book> lowStockBooks = this.findLowStockBooks();
        stats.put("lowStockCount", lowStockBooks.size());

        // 4. 按类别统计
        List<Object[]> categoryStats = bookRepository.countByCategory();
        Map<String, Long> categoryCount = new HashMap<>();
        for (Object[] row : categoryStats) {
            if (row[0] != null) { // 确保类别不为空
                categoryCount.put((String) row[0], (Long) row[1]);
            }
        }
        stats.put("booksByCategory", categoryCount);

        // 5. 总体书籍数量（包含非激活）
        stats.put("totalBooks", bookRepository.count());

        logger.info("书籍统计信息已生成，活跃书籍: {} 本", activeCount);
        return stats;
    }

    // 新增方法：计算库存总价值（使用数据库聚合函数，性能更优）
    public BigDecimal calculateTotalInventoryValue() {
        // 使用数据库聚合函数计算，避免加载所有数据到内存
        BigDecimal totalValue = bookRepository.calculateTotalInventoryValue();
        logger.debug("库存总价值计算完成: {}", totalValue);
        return totalValue;
    }

    private void validateBook(Book book) {
        if (book.getPurchasePrice() != null && book.getSellingPrice() != null) {
            if (book.getSellingPrice().compareTo(book.getPurchasePrice()) < 0) {
                throw new BusinessException("PRICE_VALIDATION", "售价不能低于进价");
            }
        }

        if (book.getStockQuantity() != null && book.getStockQuantity() < 0) {
            throw new BusinessException("STOCK_VALIDATION", "库存数量不能为负数");
        }

        if (book.getMinStock() != null && book.getMinStock() < 0) {
            throw new BusinessException("MIN_STOCK_VALIDATION", "最低库存不能为负数");
        }
    }
}