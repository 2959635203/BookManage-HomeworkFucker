package com.northgod.server.repository;

import com.northgod.server.entity.Transaction;
import com.northgod.server.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTransactionType(TransactionType transactionType);

    @Query("SELECT t FROM Transaction t WHERE DATE(t.createdAt) = :date ORDER BY t.createdAt DESC")
    List<Transaction> findByDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t WHERE t.book.id = :bookId ORDER BY t.createdAt DESC")
    Page<Transaction> findByBookId(@Param("bookId") Long bookId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month ORDER BY t.createdAt DESC")
    List<Transaction> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT t FROM Transaction t WHERE t.transactionType = 'SALE' AND YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month")
    List<Transaction> findSalesByMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
        SELECT t.book, SUM(t.quantity) as totalQuantity, SUM(t.totalAmount) as totalAmount 
        FROM Transaction t 
        WHERE t.transactionType = 'SALE' 
        AND YEAR(t.createdAt) = :year 
        AND MONTH(t.createdAt) = :month 
        GROUP BY t.book 
        ORDER BY totalQuantity DESC
    """)
    List<Object[]> findSalesRanking(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.transactionType = 'SALE' AND DATE(t.createdAt) = :date")
    BigDecimal getDailySalesTotal(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.transactionType = 'PURCHASE' AND DATE(t.createdAt) = :date")
    BigDecimal getDailyPurchasesTotal(@Param("date") LocalDate date);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE DATE(t.createdAt) = :date")
    long countByDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    @Query("""
        SELECT 
            DATE(t.createdAt) as transactionDate,
            COUNT(t) as transactionCount,
            SUM(CASE WHEN t.transactionType = 'SALE' THEN t.totalAmount ELSE 0 END) as salesTotal,
            SUM(CASE WHEN t.transactionType = 'PURCHASE' THEN t.totalAmount ELSE 0 END) as purchasesTotal
        FROM Transaction t
        WHERE t.createdAt >= :startDate AND t.createdAt < :endDate
        GROUP BY DATE(t.createdAt)
        ORDER BY DATE(t.createdAt) DESC
    """)
    List<Object[]> getDailySummary(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    Optional<Transaction> findByIdAndTransactionType(Long id, TransactionType transactionType);
}