package com.northgod.client.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Transaction {
    private Long id;
    private Book book;
    private String transactionType; // PURCHASE, SALE, RETURN
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Supplier relatedSupplier;
    private Long relatedTransactionId;
    private String operatorName;
    private String notes;
    private LocalDateTime createdAt;
}