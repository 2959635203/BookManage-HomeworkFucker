package com.northgod.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.northgod.server.entity.Book;
import com.northgod.server.entity.Supplier;
import com.northgod.server.entity.Transaction;
import com.northgod.server.enums.TransactionType;
import com.northgod.server.exception.BusinessException;
import com.northgod.server.repository.BookRepository;
import com.northgod.server.repository.SupplierRepository;
import com.northgod.server.repository.TransactionRepository;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;
    private final BookService bookService;
    private final BookRepository bookRepository;
    private final SupplierRepository supplierRepository;
    private final CacheService cacheService;

    public TransactionService(TransactionRepository transactionRepository,
                              BookService bookService,
                              BookRepository bookRepository,
                              SupplierRepository supplierRepository,
                              CacheService cacheService) {
        this.transactionRepository = transactionRepository;
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        this.supplierRepository = supplierRepository;
        this.cacheService = cacheService;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = "transactions", allEntries = true)
    public Transaction createPurchase(Transaction transaction) {
        validateTransaction(transaction, TransactionType.PURCHASE);

        // 先获取bookId，用于后续清除缓存
        Long bookId = transaction.getBook() != null ? transaction.getBook().getId() : null;
        if (bookId == null) {
            throw new BusinessException("MISSING_BOOK", "书籍信息不能为空");
        }

        // 从数据库重新加载Book实体，避免版本控制问题
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));
        transaction.setBook(book);

        // 从数据库重新加载Supplier实体
        if (transaction.getRelatedSupplier() == null || transaction.getRelatedSupplier().getId() == null) {
            throw new BusinessException("MISSING_SUPPLIER", "进货必须指定供应商");
        }
        Supplier supplier = supplierRepository.findById(transaction.getRelatedSupplier().getId())
                .orElseThrow(() -> new BusinessException("SUPPLIER_NOT_FOUND", "供应商不存在"));
        transaction.setRelatedSupplier(supplier);

        transaction.setTransactionType(TransactionType.PURCHASE);

        if (transaction.getUnitPrice() == null || transaction.getQuantity() == null) {
            throw new BusinessException("MISSING_REQUIRED_FIELDS", "单价和数量不能为空");
        }

        BigDecimal total = transaction.getUnitPrice()
                .multiply(BigDecimal.valueOf(transaction.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        transaction.setTotalAmount(total);

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("创建进货记录: {}, 金额: {}", savedTransaction.getId(), total);

        // 手动清除书籍缓存
        cacheService.evictBookCache(bookId);

        bookService.updateStock(bookId, transaction.getQuantity());

        return savedTransaction;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = "transactions", allEntries = true)
    public Transaction createSale(Transaction transaction) {
        validateTransaction(transaction, TransactionType.SALE);

        // 先获取bookId，用于后续清除缓存
        Long bookId = transaction.getBook() != null ? transaction.getBook().getId() : null;
        if (bookId == null) {
            throw new BusinessException("MISSING_BOOK", "书籍信息不能为空");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));

        // 从数据库重新加载Book实体，避免版本控制问题
        transaction.setBook(book);

        if (book.getStockQuantity() < transaction.getQuantity()) {
            throw new BusinessException("INSUFFICIENT_STOCK",
                    String.format("库存不足，当前库存：%d，请求数量：%d",
                            book.getStockQuantity(), transaction.getQuantity()));
        }

        transaction.setTransactionType(TransactionType.SALE);

        if (transaction.getUnitPrice() == null || transaction.getQuantity() == null) {
            throw new BusinessException("MISSING_REQUIRED_FIELDS", "单价和数量不能为空");
        }

        if (book.getSellingPrice() != null &&
                transaction.getUnitPrice().compareTo(book.getSellingPrice()) > 0) {
            logger.warn("销售价格高于标价，书籍ID: {}, 标价: {}, 售价: {}",
                    book.getId(), book.getSellingPrice(), transaction.getUnitPrice());
        }

        BigDecimal total = transaction.getUnitPrice()
                .multiply(BigDecimal.valueOf(transaction.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        transaction.setTotalAmount(total);

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("创建销售记录: {}, 金额: {}", savedTransaction.getId(), total);

        // 手动清除书籍缓存
        cacheService.evictBookCache(bookId);

        bookService.updateStock(bookId, -transaction.getQuantity());

        return savedTransaction;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(value = "transactions", allEntries = true)
    public Transaction createReturn(Transaction transaction) {
        if (transaction.getRelatedTransactionId() == null) {
            throw new BusinessException("MISSING_REFERENCE", "必须指定原销售记录ID");
        }

        Transaction originalTransaction = transactionRepository
                .findByIdAndTransactionType(transaction.getRelatedTransactionId(), TransactionType.SALE)
                .orElseThrow(() -> new BusinessException("ORIGINAL_TRANSACTION_NOT_FOUND", "原销售记录不存在"));

        if (transaction.getQuantity() > originalTransaction.getQuantity()) {
            throw new BusinessException("EXCESSIVE_RETURN",
                    String.format("退货数量不能超过原销售数量，原销售数量：%d",
                            originalTransaction.getQuantity()));
        }

        validateTransaction(transaction, TransactionType.RETURN);
        
        // 先获取bookId，用于后续清除缓存
        Long bookId = transaction.getBook() != null ? transaction.getBook().getId() : null;
        if (bookId == null) {
            throw new BusinessException("MISSING_BOOK", "书籍信息不能为空");
        }
        
        // 从数据库重新加载Book实体，避免版本控制问题
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));
        transaction.setBook(book);
        
        transaction.setTransactionType(TransactionType.RETURN);

        BigDecimal total = transaction.getUnitPrice()
                .multiply(BigDecimal.valueOf(transaction.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        transaction.setTotalAmount(total);

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("创建退货记录: {}, 关联销售记录: {}",
                savedTransaction.getId(), transaction.getRelatedTransactionId());

        // 手动清除书籍缓存
        cacheService.evictBookCache(bookId);

        bookService.updateStock(bookId, transaction.getQuantity());

        return savedTransaction;
    }

    public List<Transaction> getTodayTransactions() {
        return transactionRepository.findByDate(LocalDate.now());
    }

    public Page<Transaction> getTransactionHistory(Long bookId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return transactionRepository.findByBookId(bookId, pageRequest);
    }

    public List<Transaction> getMonthlyTransactions(int year, int month) {
        return transactionRepository.findByYearAndMonth(year, month);
    }

    public Map<String, Object> getMonthlySummary(int year, int month) {
        List<Transaction> monthlyTransactions = getMonthlyTransactions(year, month);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("year", year);
        summary.put("month", month);

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal totalReturns = BigDecimal.ZERO;
        int saleQuantity = 0;
        int purchaseQuantity = 0;
        int returnQuantity = 0;

        for (Transaction t : monthlyTransactions) {
            switch (t.getTransactionType()) {
                case SALE -> {
                    totalSales = totalSales.add(t.getTotalAmount());
                    saleQuantity += t.getQuantity();
                }
                case PURCHASE -> {
                    totalPurchases = totalPurchases.add(t.getTotalAmount());
                    purchaseQuantity += t.getQuantity();
                }
                case RETURN -> {
                    totalReturns = totalReturns.add(t.getTotalAmount());
                    returnQuantity += t.getQuantity();
                }
            }
        }

        summary.put("totalSales", totalSales);
        summary.put("totalPurchases", totalPurchases);
        summary.put("totalReturns", totalReturns);
        summary.put("saleQuantity", saleQuantity);
        summary.put("purchaseQuantity", purchaseQuantity);
        summary.put("returnQuantity", returnQuantity);
        summary.put("netRevenue", totalSales.subtract(totalPurchases).subtract(totalReturns));
        summary.put("transactionCount", monthlyTransactions.size());

        return summary;
    }

    public List<Map<String, Object>> getSalesRanking(int year, int month) {
        List<Object[]> results = transactionRepository.findSalesRanking(year, month);

        return results.stream()
                .limit(10) // 只取前10名
                .map(row -> {
                    Book book = (Book) row[0];
                    Long quantity = (Long) row[1];
                    BigDecimal amount = (BigDecimal) row[2];

                    Map<String, Object> ranking = new LinkedHashMap<>();
                    ranking.put("rank", results.indexOf(row) + 1);
                    ranking.put("bookId", book.getId());
                    ranking.put("title", book.getTitle());
                    ranking.put("author", book.getAuthor());
                    ranking.put("quantity", quantity);
                    ranking.put("amount", amount);
                    ranking.put("averagePrice",
                            amount.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP));
                    return ranking;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getDailySummary(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Object[]> dailyResults = transactionRepository.getDailySummary(start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> dailySummaries = new ArrayList<>();

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        long totalTransactions = 0;

        for (Object[] row : dailyResults) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];
            BigDecimal sales = (BigDecimal) row[2];
            BigDecimal purchases = (BigDecimal) row[3];

            Map<String, Object> daily = new LinkedHashMap<>();
            daily.put("date", date);
            daily.put("transactionCount", count);
            daily.put("salesTotal", sales);
            daily.put("purchasesTotal", purchases);
            daily.put("netTotal", sales.subtract(purchases));

            dailySummaries.add(daily);

            totalSales = totalSales.add(sales);
            totalPurchases = totalPurchases.add(purchases);
            totalTransactions += count;
        }

        result.put("period", Map.of("startDate", startDate, "endDate", endDate));
        result.put("dailySummaries", dailySummaries);
        result.put("summary", Map.of(
                "totalSales", totalSales,
                "totalPurchases", totalPurchases,
                "netTotal", totalSales.subtract(totalPurchases),
                "totalTransactions", totalTransactions,
                "averageDailySales", totalSales.divide(BigDecimal.valueOf(dailySummaries.size()), 2, RoundingMode.HALF_UP)
        ));

        return result;
    }

    public BigDecimal getDailySalesTotal(LocalDate date) {
        return transactionRepository.getDailySalesTotal(date);
    }

    public BigDecimal getDailyPurchasesTotal(LocalDate date) {
        return transactionRepository.getDailyPurchasesTotal(date);
    }

    /**
     * 获取推荐进货数量
     * 根据过去30天的销售数据和当前库存计算推荐进货数量
     */
    public Map<String, Object> getRecommendedPurchaseQuantity(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));

        int currentStock = book.getStockQuantity();
        int minStock = Optional.ofNullable(book.getMinStock()).orElse(0);

        // 计算过去30天的销售数据
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Transaction> recentSales = transactionRepository.findByDateRange(
                startDateTime, endDateTime, PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(t -> t.getTransactionType() == TransactionType.SALE && 
                             t.getBook().getId().equals(bookId))
                .collect(Collectors.toList());

        // 计算总销售数量和平均日销量
        int totalSalesQuantity = recentSales.stream()
                .mapToInt(Transaction::getQuantity)
                .sum();
        double averageDailySales = totalSalesQuantity / 30.0;

        // 计算推荐进货数量
        // 策略：保证至少30天的库存，同时考虑最低库存要求
        int recommendedQuantity;
        String reason;

        if (recentSales.isEmpty()) {
            // 没有销售历史，建议保持最低库存的2倍
            recommendedQuantity = Math.max(minStock * 2 - currentStock, minStock);
            reason = "该书籍暂无销售历史，建议保持最低库存的2倍";
        } else if (averageDailySales <= 0) {
            // 平均日销量为0，建议保持最低库存
            recommendedQuantity = Math.max(minStock - currentStock, 0);
            reason = "过去30天无销售记录，建议保持最低库存";
        } else {
            // 根据平均日销量计算，保证至少30天的库存
            int targetStock = (int) Math.ceil(averageDailySales * 30);
            // 确保不低于最低库存的2倍
            targetStock = Math.max(targetStock, minStock * 2);
            recommendedQuantity = Math.max(targetStock - currentStock, 0);
            
            if (recommendedQuantity == 0) {
                reason = String.format("当前库存充足（%d本），过去30天平均日销量%.1f本，建议暂不进货",
                        currentStock, averageDailySales);
            } else {
                reason = String.format("过去30天销售%d本，平均日销量%.1f本，当前库存%d本，建议进货%d本以保证30天库存",
                        totalSalesQuantity, averageDailySales, currentStock, recommendedQuantity);
            }
        }

        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("recommendedQuantity", recommendedQuantity);
        recommendation.put("reason", reason);
        recommendation.put("currentStock", currentStock);
        recommendation.put("minStock", minStock);
        recommendation.put("totalSalesLast30Days", totalSalesQuantity);
        recommendation.put("averageDailySales", Math.round(averageDailySales * 10.0) / 10.0);

        return recommendation;
    }

    private void validateTransaction(Transaction transaction, TransactionType expectedType) {
        if (transaction.getBook() == null || transaction.getBook().getId() == null) {
            throw new BusinessException("MISSING_BOOK", "书籍信息不能为空");
        }

        if (transaction.getQuantity() == null || transaction.getQuantity() <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "数量必须大于0");
        }

        if (transaction.getUnitPrice() == null || transaction.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_PRICE", "单价必须大于0");
        }

        // 验证书籍是否存在且可用
        Book book = bookRepository.findById(transaction.getBook().getId())
                .orElseThrow(() -> new BusinessException("BOOK_NOT_FOUND", "书籍不存在"));

        if (!book.getIsActive()) {
            throw new BusinessException("BOOK_INACTIVE", "书籍已下架");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void voidTransaction(Long transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "交易记录不存在"));

        // 只能作废当天的交易
        if (!transaction.getCreatedAt().toLocalDate().equals(LocalDate.now())) {
            throw new BusinessException("CANNOT_VOID_OLD_TRANSACTION", "只能作废当天的交易记录");
        }

        // 反向操作恢复库存
        int reverseQuantity = switch (transaction.getTransactionType()) {
            case SALE -> transaction.getQuantity(); // 销售作废，增加库存
            case PURCHASE -> -transaction.getQuantity(); // 进货作废，减少库存
            case RETURN -> -transaction.getQuantity(); // 退货作废，减少库存
        };

        try {
            bookService.updateStock(transaction.getBook().getId(), reverseQuantity);
            transaction.setNotes((transaction.getNotes() != null ? transaction.getNotes() + "\n" : "") +
                    String.format("[作废] %s 原因: %s", LocalDateTime.now(), reason));
            transactionRepository.save(transaction);

            logger.info("交易记录已作废: {}, 原因: {}", transactionId, reason);
        } catch (Exception e) {
            throw new BusinessException("VOID_TRANSACTION_FAILED", "作废交易失败: " + e.getMessage());
        }
    }
}