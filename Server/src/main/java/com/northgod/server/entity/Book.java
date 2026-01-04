package com.northgod.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "book", indexes = {
        @Index(name = "idx_book_isbn", columnList = "isbn", unique = true),
        @Index(name = "idx_book_title", columnList = "title"),
        @Index(name = "idx_book_author", columnList = "author"),
        @Index(name = "idx_book_created_at", columnList = "created_at"),
        @Index(name = "idx_book_stock", columnList = "stock_quantity")
})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
    @SequenceGenerator(name = "book_seq", sequenceName = "book_id_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "ISBN不能为空")
    @Column(unique = true, nullable = false, length = 20)
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(length = 100)
    private String publisher;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;  // 进价

    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice;   // 售价

    @NotNull(message = "库存数量不能为空")
    @PositiveOrZero(message = "库存数量不能为负数")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @NotNull(message = "最低库存不能为空")
    @PositiveOrZero(message = "最低库存不能为负数")
    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 10;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // 添加与 Transaction 的关系
    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Transaction> transactions;

    public boolean isLowStock() {
        return stockQuantity <= minStock;
    }

    public BigDecimal calculatePotentialProfit() {
        if (sellingPrice != null && purchasePrice != null && stockQuantity != null) {
            return sellingPrice.subtract(purchasePrice)
                    .multiply(BigDecimal.valueOf(stockQuantity));
        }
        return BigDecimal.ZERO;
    }
}