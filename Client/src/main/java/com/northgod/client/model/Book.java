package com.northgod.client.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Book {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private Integer stockQuantity;
    private Integer minStock;

    // 为了方便显示，重写toString方法
    @Override
    public String toString() {
        String titleStr = title != null ? title : "未知";
        String isbnStr = isbn != null ? isbn : "";
        int stock = stockQuantity != null ? stockQuantity : 0;
        return String.format("%s (ISBN: %s, 库存: %d)", titleStr, isbnStr, stock);
    }
}