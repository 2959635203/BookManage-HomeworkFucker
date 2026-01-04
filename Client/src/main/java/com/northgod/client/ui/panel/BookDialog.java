package com.northgod.client.ui.panel;

import com.northgod.client.model.Book;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class BookDialog extends JDialog {
    private Book book;
    private boolean confirmed = false;

    private JTextField isbnField;
    private JTextField titleField;
    private JTextField authorField;
    private JTextField publisherField;
    private JTextField purchasePriceField;
    private JTextField sellingPriceField;
    private JTextField stockQuantityField;
    private JTextField minStockField;

    public BookDialog(Frame parent, String title, Book book) {
        super(parent, title, true);
        this.book = book;
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 表单面板
        JPanel formPanel = new JPanel(new GridLayout(9, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建字段
        isbnField = new JTextField(20);
        titleField = new JTextField(20);
        authorField = new JTextField(20);
        publisherField = new JTextField(20);
        purchasePriceField = new JTextField(20);
        sellingPriceField = new JTextField(20);
        stockQuantityField = new JTextField(20);
        minStockField = new JTextField(20);

        // 如果传入的book不为null，填充数据
        if (book != null) {
            isbnField.setText(book.getIsbn());
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            publisherField.setText(book.getPublisher());
            purchasePriceField.setText(book.getPurchasePrice().toString());
            sellingPriceField.setText(book.getSellingPrice().toString());
            stockQuantityField.setText(book.getStockQuantity().toString());
            minStockField.setText(book.getMinStock().toString());
        }

        // 添加标签和字段（必填字段添加星号）
        JLabel isbnLabel = new JLabel("ISBN:*");
        isbnLabel.setForeground(Color.RED);
        formPanel.add(isbnLabel);
        formPanel.add(isbnField);

        JLabel titleLabel = new JLabel("书名:*");
        titleLabel.setForeground(Color.RED);
        formPanel.add(titleLabel);
        formPanel.add(titleField);

        formPanel.add(new JLabel("作者:"));
        formPanel.add(authorField);

        formPanel.add(new JLabel("出版社:"));
        formPanel.add(publisherField);

        JLabel purchasePriceLabel = new JLabel("进价:*");
        purchasePriceLabel.setForeground(Color.RED);
        formPanel.add(purchasePriceLabel);
        formPanel.add(purchasePriceField);

        JLabel sellingPriceLabel = new JLabel("售价:*");
        sellingPriceLabel.setForeground(Color.RED);
        formPanel.add(sellingPriceLabel);
        formPanel.add(sellingPriceField);

        JLabel stockQuantityLabel = new JLabel("库存:*");
        stockQuantityLabel.setForeground(Color.RED);
        formPanel.add(stockQuantityLabel);
        formPanel.add(stockQuantityField);

        JLabel minStockLabel = new JLabel("最低库存:*");
        minStockLabel.setForeground(Color.RED);
        formPanel.add(minStockLabel);
        formPanel.add(minStockField);

        add(formPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // 事件处理
        okButton.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        // 设置大小
        setSize(400, 350);
    }

    private boolean validateInput() {
        // 验证必填字段
        if (isbnField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "ISBN不能为空，请填写ISBN字段", "输入错误", JOptionPane.ERROR_MESSAGE);
            isbnField.requestFocus();
            return false;
        }

        if (titleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "书名不能为空，请填写书名字段", "输入错误", JOptionPane.ERROR_MESSAGE);
            titleField.requestFocus();
            return false;
        }

        // 验证数值字段，具体指出哪个字段有问题
        try {
            String purchasePriceText = purchasePriceField.getText().trim();
            if (purchasePriceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "进价不能为空，请填写进价字段", "输入错误", JOptionPane.ERROR_MESSAGE);
                purchasePriceField.requestFocus();
                return false;
            }
            new BigDecimal(purchasePriceText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "进价格式不正确，请输入有效的数字（例如：29.99）", "输入错误", JOptionPane.ERROR_MESSAGE);
            purchasePriceField.requestFocus();
            return false;
        }

        try {
            String sellingPriceText = sellingPriceField.getText().trim();
            if (sellingPriceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "售价不能为空，请填写售价字段", "输入错误", JOptionPane.ERROR_MESSAGE);
                sellingPriceField.requestFocus();
                return false;
            }
            new BigDecimal(sellingPriceText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "售价格式不正确，请输入有效的数字（例如：39.99）", "输入错误", JOptionPane.ERROR_MESSAGE);
            sellingPriceField.requestFocus();
            return false;
        }

        try {
            String stockQuantityText = stockQuantityField.getText().trim();
            if (stockQuantityText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "库存不能为空，请填写库存字段", "输入错误", JOptionPane.ERROR_MESSAGE);
                stockQuantityField.requestFocus();
                return false;
            }
            int stock = Integer.parseInt(stockQuantityText);
            if (stock < 0) {
                JOptionPane.showMessageDialog(this, "库存不能为负数，请输入大于等于0的整数", "输入错误", JOptionPane.ERROR_MESSAGE);
                stockQuantityField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "库存格式不正确，请输入有效的整数（例如：100）", "输入错误", JOptionPane.ERROR_MESSAGE);
            stockQuantityField.requestFocus();
            return false;
        }

        try {
            String minStockText = minStockField.getText().trim();
            if (minStockText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "最低库存不能为空，请填写最低库存字段", "输入错误", JOptionPane.ERROR_MESSAGE);
                minStockField.requestFocus();
                return false;
            }
            int minStock = Integer.parseInt(minStockText);
            if (minStock < 0) {
                JOptionPane.showMessageDialog(this, "最低库存不能为负数，请输入大于等于0的整数", "输入错误", JOptionPane.ERROR_MESSAGE);
                minStockField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "最低库存格式不正确，请输入有效的整数（例如：10）", "输入错误", JOptionPane.ERROR_MESSAGE);
            minStockField.requestFocus();
            return false;
        }

        return true;
    }

    public Book getBook() {
        if (!confirmed) {
            return null;
        }

        Book book = new Book();
        book.setIsbn(isbnField.getText().trim());
        book.setTitle(titleField.getText().trim());
        book.setAuthor(authorField.getText().trim());
        book.setPublisher(publisherField.getText().trim());
        book.setPurchasePrice(new BigDecimal(purchasePriceField.getText().trim()));
        book.setSellingPrice(new BigDecimal(sellingPriceField.getText().trim()));
        book.setStockQuantity(Integer.parseInt(stockQuantityField.getText().trim()));
        book.setMinStock(Integer.parseInt(minStockField.getText().trim()));

        return book;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}