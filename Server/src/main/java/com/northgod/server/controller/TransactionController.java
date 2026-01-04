package com.northgod.server.controller;

import com.northgod.server.entity.Transaction;
import com.northgod.server.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易控制器 - 已适配 Spring Boot 4.0
 * 完整保留所有业务方法，直接初始化 RestClient 避免启动冲突
 */
@RestController
@RequestMapping("/transactions")
@Validated
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;
    private final RestClient restClient;

    /**
     * 构造函数
     * @param transactionService 交易服务
     */
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
        // 直接初始化 RestClient，避免复杂的 Bean 依赖问题
        this.restClient = RestClient.builder().build();
    }

    /**
     * 创建进货记录
     */
    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> createPurchase(@Valid @RequestBody Transaction transaction) {
        try {
            logger.debug("创建进货记录，书籍ID: {}", transaction.getBook() != null ? transaction.getBook().getId() : "null");
            Transaction created = transactionService.createPurchase(transaction);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "进货成功");
            
            // 构建简化的交易数据，避免循环引用
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("id", created.getId());
            transactionData.put("transactionType", created.getTransactionType().toString());
            transactionData.put("quantity", created.getQuantity());
            transactionData.put("unitPrice", created.getUnitPrice());
            transactionData.put("totalAmount", created.getTotalAmount());
            transactionData.put("notes", created.getNotes());
            transactionData.put("createdAt", created.getCreatedAt());
            
            // 添加书籍基本信息
            if (created.getBook() != null) {
                Map<String, Object> bookData = new HashMap<>();
                bookData.put("id", created.getBook().getId());
                bookData.put("title", created.getBook().getTitle());
                bookData.put("isbn", created.getBook().getIsbn());
                transactionData.put("book", bookData);
            }
            
            // 添加供应商基本信息
            if (created.getRelatedSupplier() != null) {
                Map<String, Object> supplierData = new HashMap<>();
                supplierData.put("id", created.getRelatedSupplier().getId());
                supplierData.put("name", created.getRelatedSupplier().getName());
                transactionData.put("relatedSupplier", supplierData);
            }
            
            response.put("data", transactionData);
            response.put("transactionId", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("创建进货记录失败", e);
            return createErrorResponse("进货失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 创建销售记录
     */
    @PostMapping("/sale")
    public ResponseEntity<Map<String, Object>> createSale(@Valid @RequestBody Transaction transaction) {
        try {
            logger.debug("创建销售记录，书籍ID: {}", transaction.getBook() != null ? transaction.getBook().getId() : "null");
            Transaction created = transactionService.createSale(transaction);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "销售成功");
            response.put("data", created);
            response.put("transactionId", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("创建销售记录失败", e);
            return createErrorResponse("销售失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 创建退货记录
     */
    @PostMapping("/return")
    public ResponseEntity<Map<String, Object>> createReturn(@Valid @RequestBody Transaction transaction) {
        try {
            logger.debug("创建退货记录，原交易ID: {}", transaction.getRelatedTransactionId());
            Transaction created = transactionService.createReturn(transaction);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "退货成功");
            response.put("data", created);
            response.put("transactionId", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("创建退货记录失败", e);
            return createErrorResponse("退货失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 获取今日交易记录
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayTransactions() {
        try {
            logger.debug("获取今日交易记录");
            List<Transaction> transactions = transactionService.getTodayTransactions();
            BigDecimal salesTotal = transactionService.getDailySalesTotal(LocalDate.now());
            BigDecimal purchasesTotal = transactionService.getDailyPurchasesTotal(LocalDate.now());
            
            // 构建包含完整信息的交易数据列表，避免LAZY加载问题
            List<Map<String, Object>> transactionDataList = new ArrayList<>();
            for (Transaction transaction : transactions) {
                Map<String, Object> transactionData = new HashMap<>();
                transactionData.put("id", transaction.getId());
                transactionData.put("transactionType", transaction.getTransactionType().toString());
                transactionData.put("quantity", transaction.getQuantity());
                transactionData.put("unitPrice", transaction.getUnitPrice());
                transactionData.put("totalAmount", transaction.getTotalAmount());
                transactionData.put("notes", transaction.getNotes());
                transactionData.put("createdAt", transaction.getCreatedAt());
                
                // 添加关联交易ID（如果是退货记录）
                if (transaction.getRelatedTransactionId() != null) {
                    transactionData.put("relatedTransactionId", transaction.getRelatedTransactionId());
                }
                
                // 添加书籍信息
                if (transaction.getBook() != null) {
                    Map<String, Object> bookData = new HashMap<>();
                    bookData.put("id", transaction.getBook().getId());
                    bookData.put("title", transaction.getBook().getTitle());
                    bookData.put("isbn", transaction.getBook().getIsbn());
                    bookData.put("stockQuantity", transaction.getBook().getStockQuantity());
                    transactionData.put("book", bookData);
                }
                
                // 添加供应商信息（如果是进货记录）
                if (transaction.getRelatedSupplier() != null) {
                    Map<String, Object> supplierData = new HashMap<>();
                    supplierData.put("id", transaction.getRelatedSupplier().getId());
                    supplierData.put("name", transaction.getRelatedSupplier().getName());
                    transactionData.put("relatedSupplier", supplierData);
                }
                
                transactionDataList.add(transactionData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transactionDataList);
            response.put("summary", Map.of(
                    "date", LocalDate.now(),
                    "totalTransactions", transactions.size(),
                    "salesTotal", salesTotal,
                    "purchasesTotal", purchasesTotal,
                    "netTotal", salesTotal.subtract(purchasesTotal)
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取今日交易记录失败", e);
            return createErrorResponse("获取今日交易记录失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取月度交易记录
     */
    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlyTransactions(
            @PathVariable("year") int year,
            @PathVariable("month") @Min(1) @Max(12) int month) {
        try {
            logger.debug("获取月度交易记录，{}-{}", year, month);
            List<Transaction> transactions = transactionService.getMonthlyTransactions(year, month);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transactions);
            response.put("period", Map.of("year", year, "month", month));
            response.put("count", transactions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取月度交易记录失败，{}-{}", year, month, e);
            return createErrorResponse("获取月度交易记录失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取月度汇总
     */
    @GetMapping("/summary/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @PathVariable("year") int year,
            @PathVariable("month") @Min(1) @Max(12) int month) {
        try {
            logger.debug("获取月度汇总，{}-{}", year, month);
            Map<String, Object> summary = transactionService.getMonthlySummary(year, month);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取月度汇总失败，{}-{}", year, month, e);
            return createErrorResponse("获取月度汇总失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取销售排行
     */
    @GetMapping("/ranking/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getSalesRanking(
            @PathVariable("year") int year,
            @PathVariable("month") @Min(1) @Max(12) int month) {
        try {
            logger.debug("获取销售排行，{}-{}", year, month);
            List<Map<String, Object>> ranking = transactionService.getSalesRanking(year, month);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ranking);
            response.put("period", Map.of("year", year, "month", month));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取销售排行失败，{}-{}", year, month, e);
            return createErrorResponse("获取销售排行失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取每日汇总
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            logger.debug("获取每日汇总，{} 到 {}", startDate, endDate);
            if (startDate.isAfter(endDate)) {
                return createErrorResponse("开始日期不能晚于结束日期", HttpStatus.BAD_REQUEST);
            }
            if (startDate.plusMonths(3).isBefore(endDate)) {
                return createErrorResponse("查询时间范围不能超过3个月", HttpStatus.BAD_REQUEST);
            }
            Map<String, Object> summary = transactionService.getDailySummary(startDate, endDate);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取每日汇总失败，{} 到 {}", startDate, endDate, e);
            return createErrorResponse("获取每日汇总失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取推荐进货数量
     */
    @GetMapping("/purchase-recommendation/{bookId}")
    public ResponseEntity<Map<String, Object>> getPurchaseRecommendation(@PathVariable("bookId") Long bookId) {
        try {
            logger.debug("获取推荐进货数量，书籍ID: {}", bookId);
            Map<String, Object> recommendation = transactionService.getRecommendedPurchaseQuantity(bookId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", recommendation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取推荐进货数量失败，书籍ID: {}", bookId, e);
            return createErrorResponse("获取推荐进货数量失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 作废交易记录
     */
    @PutMapping("/{id}/void")
    public ResponseEntity<Map<String, Object>> voidTransaction(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = true) String reason) {
        try {
            logger.debug("作废交易记录，ID: {}, 原因: {}", id, reason);
            transactionService.voidTransaction(id, reason);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "交易记录已作废");
            response.put("transactionId", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("作废交易记录失败，ID: {}", id, e);
            return createErrorResponse("作废交易记录失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 示例：使用 RestClient 调用外部服务验证交易
     * 此方法演示了 Spring Boot 4.0 中 RestClient 的用法
     */
    @GetMapping("/{id}/external-verify")
    public ResponseEntity<Map<String, Object>> verifyWithExternalSystem(@PathVariable("id") Long id) {
        try {
            logger.debug("调用外部系统验证交易，交易ID: {}", id);

            // 使用 Spring Boot 4.0 的 RestClient
            String externalResponse = restClient
                    .get()
                    .uri("https://api.example-external.com/transactions/{id}/verify", id)
                    .header("X-API-Key", "your-api-key-if-required")
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new RuntimeException("外部服务未找到该交易记录");
                    })
                    .body(String.class);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", id);
            response.put("externalVerification", externalResponse);
            response.put("verifiedAt", LocalDate.now());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("外部系统验证失败，交易ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("transactionId", id);
            errorResponse.put("message", "无法完成外部验证: " + e.getMessage());
            errorResponse.put("suggestion", "请检查网络连接或联系系统管理员");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * 创建错误响应（辅助方法）
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(response);
    }
}