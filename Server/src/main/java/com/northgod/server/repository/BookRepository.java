package com.northgod.server.repository;

import com.northgod.server.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // 移除有问题的 findByIdWithTransactions 方法
    // @Query("SELECT b FROM Book b LEFT JOIN FETCH b.transactions WHERE b.id = :id")
    // Optional<Book> findByIdWithTransactions(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b FROM Book b WHERE b.isbn = :isbn AND b.isActive = true")
    Optional<Book> findByIsbn(@Param("isbn") String isbn);

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    @Query("SELECT b FROM Book b WHERE b.isActive = true")
    Page<Book> findByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.isActive = false")
    Page<Book> findByIsActiveFalse(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.isActive = true AND b.stockQuantity <= b.minStock")
    List<Book> findLowStockBooks();

    @Query("SELECT b FROM Book b WHERE b.isActive = true AND b.stockQuantity <= :quantity")
    Page<Book> findByLowStock(@Param("quantity") Integer quantity, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.isActive = true AND " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "b.isbn LIKE CONCAT('%', :keyword, '%'))")
    Page<Book> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :id")
    int updateStockQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :id AND b.stockQuantity + :quantity >= 0")
    int updateStockQuantityIfSufficient(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Query(value = """
        SELECT b.* FROM book b 
        WHERE b.is_active = true 
        AND (b.title ILIKE %:keyword% OR b.author ILIKE %:keyword% OR b.isbn LIKE %:keyword%)
        ORDER BY b.created_at DESC 
        LIMIT 100
    """, nativeQuery = true)
    List<Book> searchBooksFast(@Param("keyword") String keyword);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.isActive = true")
    long countActiveBooks();

    @Query("SELECT SUM(b.stockQuantity) FROM Book b WHERE b.isActive = true")
    Long sumStockQuantity();

    @Query("SELECT b.category, COUNT(b) as count FROM Book b WHERE b.isActive = true GROUP BY b.category")
    List<Object[]> countByCategory();

    @Query("""
        SELECT b FROM Book b 
        WHERE b.isActive = true 
        AND b.updatedAt > :since
        ORDER BY b.updatedAt DESC
    """)
    List<Book> findRecentlyUpdated(@Param("since") LocalDateTime since);

    /**
     * 批量软删除书籍（优化性能）
     * 使用数据库批量更新，将isActive设置为false
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.isActive = false WHERE b.id IN :ids")
    int softDeleteBooksBatch(@Param("ids") List<Long> ids);

    /**
     * 计算库存总价值（使用数据库聚合函数，性能更优）
     */
    @Query("""
        SELECT COALESCE(SUM(b.purchasePrice * b.stockQuantity), 0) 
        FROM Book b 
        WHERE b.isActive = true 
        AND b.purchasePrice IS NOT NULL 
        AND b.stockQuantity > 0
    """)
    BigDecimal calculateTotalInventoryValue();
}