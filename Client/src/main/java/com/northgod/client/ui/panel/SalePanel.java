package com.northgod.client.ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.northgod.client.model.Book;
import com.northgod.client.model.Transaction;
import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class SalePanel extends JPanel {
    private final ApiClient apiClient;
    private final com.northgod.client.ui.MainFrame mainFrame;
    private JTable saleTable;
    private DefaultTableModel tableModel;
    private final List<Transaction> saleList;
    
    // 销售表单组件
    private JComboBox<Book> bookComboBox;
    private JTextField quantityField;
    private JTextField unitPriceField;
    private JTextField totalAmountField;
    private JTextField notesField;
    private JLabel stockLabel;
    
    private List<Book> bookList;
    
    public SalePanel(ApiClient apiClient, com.northgod.client.ui.MainFrame mainFrame) {
        this.apiClient = apiClient;
        this.mainFrame = mainFrame;
        this.saleList = new ArrayList<>();
        this.bookList = new ArrayList<>();
        initComponents();
        loadSaleHistory();
        loadBooks();
    }
    
    /**
     * 公共方法：刷新书籍下拉框
     */
    public void refreshBooks() {
        loadBooks();
    }
    
    /**
     * 公共方法：刷新销售历史记录
     */
    public void refreshSaleHistory() {
        loadSaleHistory();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 顶部：销售表单
        JPanel formPanel = createSaleForm();
        add(formPanel, BorderLayout.NORTH);

        // 中部：销售历史记录
        JPanel tablePanel = createSaleTable();
        add(tablePanel, BorderLayout.CENTER);
    }

    private JPanel createSaleForm() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("新增销售"));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 书籍选择
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("选择书籍:"), gbc);
        gbc.gridx = 1;
        bookComboBox = new JComboBox<>();
        bookComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean hasFocus) {
                if (value instanceof Book book) {
                    return super.getListCellRendererComponent(list,
                            book.getTitle() + " (库存:" + book.getStockQuantity() + ")", index, isSelected, hasFocus);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            }
        });
        bookComboBox.addActionListener(e -> updateBookInfo());
        form.add(bookComboBox, gbc);
        gbc.gridx = 2;
        stockLabel = new JLabel("");
        stockLabel.setForeground(Color.RED);
        form.add(stockLabel, gbc);

        // 数量
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("销售数量:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        quantityField = new JTextField(15);
        quantityField.addActionListener(e -> calculateTotal());
        form.add(quantityField, gbc);
        gbc.gridwidth = 1;

        // 单价
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("单价(元):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        unitPriceField = new JTextField(15);
        unitPriceField.addActionListener(e -> calculateTotal());
        form.add(unitPriceField, gbc);
        gbc.gridwidth = 1;

        // 总金额
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("总金额(元):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        totalAmountField = new JTextField(15);
        totalAmountField.setEditable(false);
        totalAmountField.setBackground(Color.LIGHT_GRAY);
        form.add(totalAmountField, gbc);
        gbc.gridwidth = 1;

        // 备注
        gbc.gridx = 0; gbc.gridy = 4;
        form.add(new JLabel("备注:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        notesField = new JTextField(30);
        form.add(notesField, gbc);
        gbc.gridwidth = 1;

        panel.add(form, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("提交销售");
        JButton resetButton = new JButton("重置");
        JButton printButton = new JButton("打印销售单");

        submitButton.addActionListener(e -> submitSale());
        resetButton.addActionListener(e -> resetForm());
        printButton.addActionListener(e -> printSaleOrder());

        buttonPanel.add(submitButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(printButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSaleTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("销售历史记录"));

        String[] columnNames = {"书籍", "数量", "单价", "总金额", "备注", "日期"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1 -> Integer.class;    // 数量
                    default -> String.class;   // 其他列都是字符串
                };
            }
        };

        saleTable = new JTable(tableModel);
        saleTable.setRowHeight(25);
        saleTable.setAutoCreateRowSorter(true);
        
        // 设置所有单元格居中对齐
        saleTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
        
        // 确保Integer类型列也居中对齐
        saleTable.setDefaultRenderer(Integer.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // 设置列宽（共6列：索引0-5）
        saleTable.getColumnModel().getColumn(0).setPreferredWidth(200); // 书籍
        saleTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // 数量
        saleTable.getColumnModel().getColumn(2).setPreferredWidth(100); // 单价
        saleTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 总金额
        saleTable.getColumnModel().getColumn(4).setPreferredWidth(200);  // 备注
        saleTable.getColumnModel().getColumn(5).setPreferredWidth(180); // 日期

        JScrollPane scrollPane = new JScrollPane(saleTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadSaleHistory());
        toolbar.add(refreshButton);
        panel.add(toolbar, BorderLayout.NORTH);

        return panel;
    }

    private void updateBookInfo() {
        Book selected = (Book) bookComboBox.getSelectedItem();
        if (selected != null) {
            stockLabel.setText("当前库存: " + selected.getStockQuantity());
            if (selected.getSellingPrice() != null) {
                unitPriceField.setText(selected.getSellingPrice().toString());
                calculateTotal();
            }
        } else {
            stockLabel.setText("");
        }
    }

    private void calculateTotal() {
        try {
            String quantityStr = quantityField.getText().trim();
            String priceStr = unitPriceField.getText().trim();
            if (!quantityStr.isEmpty() && !priceStr.isEmpty()) {
                int quantity = Integer.parseInt(quantityStr);
                BigDecimal price = new BigDecimal(priceStr);
                BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
                totalAmountField.setText(total.toString());
                
                // 检查库存
                Book selected = (Book) bookComboBox.getSelectedItem();
                if (selected != null && quantity > selected.getStockQuantity()) {
                    stockLabel.setText("库存不足！当前库存: " + selected.getStockQuantity());
                    stockLabel.setForeground(Color.RED);
                } else if (selected != null) {
                    stockLabel.setText("当前库存: " + selected.getStockQuantity());
                    stockLabel.setForeground(Color.BLACK);
                }
            } else {
                totalAmountField.setText("");
            }
        } catch (NumberFormatException e) {
            totalAmountField.setText("");
        }
    }

    private void submitSale() {
        Book selectedBook = (Book) bookComboBox.getSelectedItem();
        
        if (selectedBook == null) {
            DialogUtil.showWarningDialog(this, "请选择书籍");
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            BigDecimal unitPrice = new BigDecimal(unitPriceField.getText().trim());
            String notes = notesField.getText().trim();

            if (quantity <= 0) {
                DialogUtil.showWarningDialog(this, "销售数量必须大于0");
                return;
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                DialogUtil.showWarningDialog(this, "单价必须大于0");
                return;
            }

            // 检查库存
            if (quantity > selectedBook.getStockQuantity()) {
                DialogUtil.showWarningDialog(this, 
                        "库存不足！\n当前库存: " + selectedBook.getStockQuantity() + 
                        "\n请求数量: " + quantity);
                return;
            }

            // 创建请求数据
            Map<String, Object> requestData = new java.util.HashMap<>();
            requestData.put("book", Map.of("id", selectedBook.getId()));
            requestData.put("transactionType", "SALE");
            requestData.put("quantity", quantity);
            requestData.put("unitPrice", unitPrice);
            requestData.put("notes", notes);

            ThreadPoolManager.getInstance().submitIoTask(() -> {
                try {
                    String response = apiClient.post("/transactions/sale", requestData);
                    Map<String, Object> result = JsonUtil.parseJson(response);

                    SwingUtilities.invokeLater(() -> {
                        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                            DialogUtil.showSuccessDialog(this, "销售成功！");
                            resetForm();
                            loadSaleHistory();
                            loadBooks(); // 刷新书籍列表以更新库存
                            // 通知其他面板刷新书籍列表和书籍管理页面
                            if (mainFrame != null) {
                                mainFrame.refreshBookDropdowns();
                                mainFrame.refreshBookPanel(); // 刷新书籍管理页面
                                mainFrame.refreshReportPanel(); // 刷新统计报表
                                mainFrame.refreshSaleHistory(); // 刷新销售历史记录
                            }
                        } else {
                            String message = result != null && result.get("message") != null ?
                                    result.get("message").toString() : "未知错误";
                            DialogUtil.showErrorDialog(this, "销售失败: " + message);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        LogUtil.error("提交销售失败", e);
                        DialogUtil.showErrorDialog(this, "提交销售失败: " + e.getMessage());
                    });
                }
            });
        } catch (NumberFormatException e) {
            DialogUtil.showWarningDialog(this, "请输入有效的数字");
        }
    }

    private void resetForm() {
        bookComboBox.setSelectedIndex(-1);
        quantityField.setText("");
        unitPriceField.setText("");
        totalAmountField.setText("");
        notesField.setText("");
        stockLabel.setText("");
    }

    private void printSaleOrder() {
        int selectedRow = saleTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtil.showWarningDialog(this, "请先选择要打印的销售记录");
            return;
        }

        int modelRow = saleTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= saleList.size()) {
            DialogUtil.showErrorDialog(this, "选择的记录无效");
            return;
        }

        Transaction transaction = saleList.get(modelRow);
        printTransaction(transaction, "销售单");
    }

    private void printTransaction(Transaction transaction, String title) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("================================\n");
            content.append("        ").append(title).append("\n");
            content.append("================================\n\n");
            content.append("单据编号: ").append(transaction.getId()).append("\n");
            content.append("日期: ").append(transaction.getCreatedAt() != null ?
                    transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append("\n");
            if (transaction.getBook() != null) {
                content.append("书籍: ").append(transaction.getBook().getTitle()).append("\n");
                content.append("ISBN: ").append(transaction.getBook().getIsbn()).append("\n");
            }
            content.append("数量: ").append(transaction.getQuantity()).append("\n");
            content.append("单价: ").append(transaction.getUnitPrice()).append(" 元\n");
            content.append("总金额: ").append(transaction.getTotalAmount()).append(" 元\n");
            if (transaction.getNotes() != null && !transaction.getNotes().isEmpty()) {
                content.append("备注: ").append(transaction.getNotes()).append("\n");
            }
            content.append("\n================================\n");

            JTextArea textArea = new JTextArea(content.toString());
            // 使用支持中文的字体
            textArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            int option = JOptionPane.showConfirmDialog(this, scrollPane, title + " - 打印预览",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                textArea.print();
                DialogUtil.showSuccessDialog(this, "打印成功！");
            }
        } catch (Exception e) {
            LogUtil.error("打印失败", e);
            DialogUtil.showErrorDialog(this, "打印失败: " + e.getMessage());
        }
    }

    private void loadBooks() {
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 分页加载所有书籍（每页最多100条）
                List<Book> allBooks = new ArrayList<>();
                int page = 0;
                int size = 100;
                boolean hasMore = true;
                
                while (hasMore) {
                    String response = apiClient.get("/books?page=" + page + "&size=" + size);
                    Map<String, Object> result = JsonUtil.parseJson(response);
                    
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Object dataObj = result.get("data");
                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            
                            if (data != null && !data.isEmpty()) {
                                for (Map<String, Object> item : data) {
                                    try {
                                        Book book = new Book();
                                        if (item.get("id") != null) {
                                            book.setId(Long.valueOf(item.get("id").toString()));
                                        }
                                        book.setIsbn(item.get("isbn") != null ? item.get("isbn").toString() : "");
                                        book.setTitle(item.get("title") != null ? item.get("title").toString() : "");
                                        book.setStockQuantity(item.get("stockQuantity") != null ?
                                                Integer.valueOf(item.get("stockQuantity").toString()) : 0);
                                        if (item.get("sellingPrice") != null) {
                                            book.setSellingPrice(new BigDecimal(item.get("sellingPrice").toString()));
                                        }
                                        allBooks.add(book);
                                    } catch (Exception e) {
                                        LogUtil.error("解析书籍数据失败: " + item, e);
                                    }
                                }
                                
                                // 检查是否还有更多页
                                Map<String, Object> pagination = (Map<String, Object>) result.get("pagination");
                                if (pagination != null) {
                                    Boolean isLast = (Boolean) pagination.get("last");
                                    hasMore = isLast == null || !isLast;
                                } else {
                                    hasMore = false;
                                }
                                page++;
                            } else {
                                hasMore = false;
                            }
                        } else {
                            hasMore = false;
                        }
                    } else {
                        hasMore = false;
                    }
                }
                
                final List<Book> finalBooks = allBooks;
                SwingUtilities.invokeLater(() -> {
                    bookList.clear();
                    bookComboBox.removeAllItems();
                    
                    // 添加空选项作为默认选择
                    bookComboBox.addItem(null);
                    
                    for (Book book : finalBooks) {
                        bookList.add(book);
                        bookComboBox.addItem(book);
                    }
                    
                    // 默认选择空（第一个选项）
                    bookComboBox.setSelectedIndex(0);
                    
                    LogUtil.info("成功加载 " + bookList.size() + " 本书籍到销售选择框");
                });
            } catch (Exception e) {
                LogUtil.error("加载书籍列表失败", e);
                SwingUtilities.invokeLater(() -> {
                    DialogUtil.showErrorDialog(this, "加载书籍列表失败: " + e.getMessage());
                });
            }
        });
    }

    private void loadSaleHistory() {
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 获取今日交易记录（不使用缓存，获取最新数据）
                String response = apiClient.get("/transactions/today");
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    saleList.clear();
                    tableModel.setRowCount(0);

                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                        for (Map<String, Object> item : data) {
                            String type = item.get("transactionType") != null ? 
                                    item.get("transactionType").toString() : "";
                            if ("SALE".equals(type)) {
                                Transaction transaction = parseTransaction(item);
                                saleList.add(transaction);

                                String bookTitle = transaction.getBook() != null ? 
                                        transaction.getBook().getTitle() : "";
                                String dateStr = transaction.getCreatedAt() != null ?
                                        transaction.getCreatedAt().format(formatter) : "";

                                tableModel.addRow(new Object[]{
                                        bookTitle,
                                        transaction.getQuantity(),
                                        transaction.getUnitPrice() != null ? transaction.getUnitPrice().toString() : "0.00",
                                        transaction.getTotalAmount() != null ? transaction.getTotalAmount().toString() : "0.00",
                                        transaction.getNotes() != null ? transaction.getNotes() : "",
                                        dateStr
                                });
                            }
                        }
                    }
                });
            } catch (Exception e) {
                LogUtil.error("加载销售历史失败", e);
                SwingUtilities.invokeLater(() -> {
                    DialogUtil.showErrorDialog(this, "加载销售历史失败: " + e.getMessage());
                });
            }
        });
    }

    private Transaction parseTransaction(Map<String, Object> item) {
        Transaction transaction = new Transaction();
        transaction.setId(Long.valueOf(item.get("id").toString()));
        transaction.setQuantity(Integer.valueOf(item.get("quantity").toString()));
        transaction.setUnitPrice(new BigDecimal(item.get("unitPrice").toString()));
        if (item.get("totalAmount") != null) {
            transaction.setTotalAmount(new BigDecimal(item.get("totalAmount").toString()));
        }
        transaction.setNotes(item.get("notes") != null ? item.get("notes").toString() : null);
        transaction.setTransactionType(item.get("transactionType").toString());

        if (item.get("book") != null) {
            Map<String, Object> bookData = (Map<String, Object>) item.get("book");
            Book book = new Book();
            book.setId(Long.valueOf(bookData.get("id").toString()));
            book.setTitle(bookData.get("title") != null ? bookData.get("title").toString() : "");
            book.setIsbn(bookData.get("isbn") != null ? bookData.get("isbn").toString() : "");
            transaction.setBook(book);
        }

        if (item.get("createdAt") != null) {
            String dateStr = item.get("createdAt").toString();
            try {
                transaction.setCreatedAt(LocalDateTime.parse(dateStr.replace("Z", "").substring(0, 19)));
            } catch (Exception e) {
                // 忽略日期解析错误
            }
        }

        return transaction;
    }
}
