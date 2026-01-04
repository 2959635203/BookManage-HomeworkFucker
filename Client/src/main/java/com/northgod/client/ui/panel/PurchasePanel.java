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
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.northgod.client.model.Book;
import com.northgod.client.model.Supplier;
import com.northgod.client.model.Transaction;
import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class PurchasePanel extends JPanel {
    private final ApiClient apiClient;
    private final com.northgod.client.ui.MainFrame mainFrame;
    private JTable purchaseTable;
    private DefaultTableModel tableModel;
    private final List<Transaction> purchaseList;
    
    // 进货表单组件
    private JComboBox<Book> bookComboBox;
    private JComboBox<Supplier> supplierComboBox;
    private JTextField quantityField;
    private JTextField unitPriceField;
    private JTextField totalAmountField;
    private JTextField notesField;
    private JButton recommendButton;
    private JLabel stockLabel;
    private JLabel recommendationLabel;
    
    private List<Book> bookList;
    private List<Supplier> supplierList;

    public PurchasePanel(ApiClient apiClient, com.northgod.client.ui.MainFrame mainFrame) {
        this.apiClient = apiClient;
        this.mainFrame = mainFrame;
        this.purchaseList = new ArrayList<>();
        this.bookList = new ArrayList<>();
        this.supplierList = new ArrayList<>();
        initComponents();
        loadPurchaseHistory();
        loadBooks();
        loadSuppliers();
    }
    
    /**
     * 公共方法：刷新书籍下拉框
     */
    public void refreshBooks() {
        loadBooks();
    }
    
    /**
     * 公共方法：刷新供应商下拉框
     */
    public void refreshSuppliers() {
        loadSuppliers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 顶部：进货表单
        JPanel formPanel = createPurchaseForm();
        add(formPanel, BorderLayout.NORTH);

        // 中部：进货历史记录
        JPanel tablePanel = createPurchaseTable();
        add(tablePanel, BorderLayout.CENTER);
    }

    private JPanel createPurchaseForm() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("新增进货"));

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
        bookComboBox.addActionListener(e -> updateStockInfo());
        form.add(bookComboBox, gbc);
        gbc.gridx = 2;
        stockLabel = new JLabel("");
        form.add(stockLabel, gbc);

        // 供应商选择
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("选择供应商:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        supplierComboBox = new JComboBox<>();
        supplierComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean hasFocus) {
                if (value instanceof Supplier supplier) {
                    return super.getListCellRendererComponent(list, supplier.getName(), index, isSelected, hasFocus);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            }
        });
        form.add(supplierComboBox, gbc);
        gbc.gridwidth = 1;

        // 数量
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("进货数量:"), gbc);
        gbc.gridx = 1;
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        quantityField = new JTextField(10);
        quantityPanel.add(quantityField);
        recommendButton = new JButton("智能推荐");
        recommendButton.addActionListener(e -> getRecommendation());
        quantityPanel.add(recommendButton);
        form.add(quantityPanel, gbc);
        gbc.gridx = 2;
        recommendationLabel = new JLabel("");
        recommendationLabel.setForeground(Color.BLUE);
        form.add(recommendationLabel, gbc);

        // 单价
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("单价(元):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        unitPriceField = new JTextField(15);
        unitPriceField.addActionListener(e -> calculateTotal());
        form.add(unitPriceField, gbc);
        gbc.gridwidth = 1;

        // 总金额
        gbc.gridx = 0; gbc.gridy = 4;
        form.add(new JLabel("总金额(元):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        totalAmountField = new JTextField(15);
        totalAmountField.setEditable(false);
        totalAmountField.setBackground(Color.LIGHT_GRAY);
        form.add(totalAmountField, gbc);
        gbc.gridwidth = 1;

        // 备注
        gbc.gridx = 0; gbc.gridy = 5;
        form.add(new JLabel("备注:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        notesField = new JTextField(30);
        form.add(notesField, gbc);
        gbc.gridwidth = 1;

        // 数量变化时自动计算总金额
        quantityField.addActionListener(e -> calculateTotal());
        unitPriceField.addActionListener(e -> calculateTotal());

        panel.add(form, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("提交进货");
        JButton resetButton = new JButton("重置");
        JButton printButton = new JButton("打印进货单");

        submitButton.addActionListener(e -> submitPurchase());
        resetButton.addActionListener(e -> resetForm());
        printButton.addActionListener(e -> printPurchaseOrder());

        buttonPanel.add(submitButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(printButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPurchaseTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("进货历史记录"));

        String[] columnNames = {"书籍", "供应商", "数量", "单价", "总金额", "备注", "日期"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        purchaseTable = new JTable(tableModel);
        purchaseTable.setRowHeight(25);
        purchaseTable.setAutoCreateRowSorter(true);
        
        // 设置所有单元格居中对齐
        purchaseTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // 设置列宽（共7列：索引0-6）
        purchaseTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        purchaseTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        purchaseTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        purchaseTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        purchaseTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        purchaseTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        purchaseTable.getColumnModel().getColumn(6).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(purchaseTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadPurchaseHistory());
        toolbar.add(refreshButton);
        panel.add(toolbar, BorderLayout.NORTH);

        return panel;
    }

    private void updateStockInfo() {
        Book selected = (Book) bookComboBox.getSelectedItem();
        if (selected != null) {
            stockLabel.setText("当前库存: " + selected.getStockQuantity() + 
                    " | 最低库存: " + (selected.getMinStock() != null ? selected.getMinStock() : 0));
            if (selected.getPurchasePrice() != null) {
                unitPriceField.setText(selected.getPurchasePrice().toString());
                calculateTotal();
            }
        } else {
            stockLabel.setText("");
        }
        recommendationLabel.setText("");
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
            } else {
                totalAmountField.setText("");
            }
        } catch (NumberFormatException e) {
            totalAmountField.setText("");
        }
    }

    private void getRecommendation() {
        Book selected = (Book) bookComboBox.getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarningDialog(this, "请先选择书籍");
            return;
        }

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.get("/transactions/purchase-recommendation/" + selected.getId());
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        Integer recommendedQty = (Integer) data.get("recommendedQuantity");
                        String reason = (String) data.get("reason");
                        
                        quantityField.setText(String.valueOf(recommendedQty));
                        recommendationLabel.setText(reason);
                        calculateTotal();
                    } else {
                        DialogUtil.showErrorDialog(this, "获取推荐失败");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    LogUtil.error("获取推荐失败", e);
                    DialogUtil.showErrorDialog(this, "获取推荐失败: " + e.getMessage());
                });
            }
        });
    }

    private void submitPurchase() {
        Book selectedBook = (Book) bookComboBox.getSelectedItem();
        Supplier selectedSupplier = (Supplier) supplierComboBox.getSelectedItem();
        
        if (selectedBook == null) {
            DialogUtil.showWarningDialog(this, "请选择书籍");
            return;
        }
        if (selectedSupplier == null) {
            DialogUtil.showWarningDialog(this, "请选择供应商");
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            BigDecimal unitPrice = new BigDecimal(unitPriceField.getText().trim());
            String notes = notesField.getText().trim();

            if (quantity <= 0) {
                DialogUtil.showWarningDialog(this, "进货数量必须大于0");
                return;
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                DialogUtil.showWarningDialog(this, "单价必须大于0");
                return;
            }

            // 创建Transaction对象
            Transaction transaction = new Transaction();
            Book bookRef = new Book();
            bookRef.setId(selectedBook.getId());
            transaction.setBook(bookRef);
            transaction.setTransactionType("PURCHASE");
            transaction.setQuantity(quantity);
            transaction.setUnitPrice(unitPrice);
            transaction.setNotes(notes);
            
            Supplier supplierRef = new Supplier();
            supplierRef.setId(selectedSupplier.getId());
            // 注意：这里需要服务端支持，暂时用Map构造请求
            Map<String, Object> requestData = new java.util.HashMap<>();
            requestData.put("book", Map.of("id", selectedBook.getId()));
            requestData.put("transactionType", "PURCHASE");
            requestData.put("quantity", quantity);
            requestData.put("unitPrice", unitPrice);
            requestData.put("notes", notes);
            requestData.put("relatedSupplier", Map.of("id", selectedSupplier.getId()));

            ThreadPoolManager.getInstance().submitIoTask(() -> {
                try {
                    String response = apiClient.post("/transactions/purchase", requestData);
                    
                    // 记录响应内容以便调试（仅记录前500字符）
                    if (response != null && response.length() > 500) {
                        LogUtil.debug("响应内容过长: " + response.length() + " 字符，前500字符: " + response.substring(0, 500));
                    }
                    
                    Map<String, Object> result = JsonUtil.parseJson(response);

                    SwingUtilities.invokeLater(() -> {
                        if (result != null) {
                            // 使用安全的方式检查success字段
                            Object successObj = result.get("success");
                            boolean isSuccess = false;
                            if (successObj instanceof Boolean) {
                                isSuccess = (Boolean) successObj;
                            } else if (successObj != null) {
                                isSuccess = Boolean.parseBoolean(successObj.toString());
                            }
                            
                            if (isSuccess) {
                                DialogUtil.showSuccessDialog(this, "进货成功！");
                                resetForm();
                                loadPurchaseHistory();
                                loadBooks(); // 刷新书籍列表以更新库存
                                // 更新当前选择书籍的库存显示
                                updateStockInfo();
                                // 通知其他面板刷新书籍列表
                                if (mainFrame != null) {
                                    mainFrame.refreshBookDropdowns();
                                    mainFrame.refreshBookPanel(); // 刷新书籍管理页面
                                    mainFrame.refreshReportPanel(); // 刷新统计报表
                                }
                            } else {
                                String message = result.get("message") != null ?
                                        result.get("message").toString() : "未知错误";
                                DialogUtil.showErrorDialog(this, "进货失败: " + message);
                            }
                        } else {
                            // JSON解析失败，尝试从响应中提取错误信息
                            if (response != null) {
                                // 尝试手动解析部分响应
                                if (response.contains("\"message\"")) {
                                    try {
                                        int msgStart = response.indexOf("\"message\"");
                                        int msgValueStart = response.indexOf("\"", msgStart + 10) + 1;
                                        int msgValueEnd = response.indexOf("\"", msgValueStart);
                                        if (msgValueEnd > msgValueStart) {
                                            String errorMsg = response.substring(msgValueStart, msgValueEnd);
                                            SwingUtilities.invokeLater(() -> {
                                                DialogUtil.showErrorDialog(this, "进货失败: " + errorMsg);
                                            });
                                            return;
                                        }
                                    } catch (Exception e) {
                                        // 忽略解析错误
                                    }
                                }
                            }
                            SwingUtilities.invokeLater(() -> {
                                DialogUtil.showErrorDialog(this, "进货失败: 服务器响应格式错误，请检查服务器日志");
                            });
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        LogUtil.error("提交进货失败", e);
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("JsonParseException")) {
                            errorMsg = "服务器返回的数据格式错误，可能是数据过大或包含循环引用";
                        }
                        DialogUtil.showErrorDialog(this, "提交进货失败: " + errorMsg);
                    });
                }
            });
        } catch (NumberFormatException e) {
            DialogUtil.showWarningDialog(this, "请输入有效的数字");
        }
    }

    private void resetForm() {
        bookComboBox.setSelectedIndex(-1);
        supplierComboBox.setSelectedIndex(-1);
        quantityField.setText("");
        unitPriceField.setText("");
        totalAmountField.setText("");
        notesField.setText("");
        stockLabel.setText("");
        recommendationLabel.setText("");
    }

    private void printPurchaseOrder() {
        int selectedRow = purchaseTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtil.showWarningDialog(this, "请先选择要打印的进货记录");
            return;
        }

        int modelRow = purchaseTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= purchaseList.size()) {
            DialogUtil.showErrorDialog(this, "选择的记录无效");
            return;
        }

        Transaction transaction = purchaseList.get(modelRow);
        printTransaction(transaction, "进货单");
    }

    private void printTransaction(Transaction transaction, String title) {
        try {
            // 创建打印内容
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

            // 显示打印预览对话框
            JTextArea textArea = new JTextArea(content.toString());
            // 使用支持中文的字体
            textArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            int option = JOptionPane.showConfirmDialog(this, scrollPane, title + " - 打印预览",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                // 执行打印
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
                    String response = apiClient.get("/books?page=" + page + "&size=" + size); // 不使用缓存参数
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
                                        book.setMinStock(item.get("minStock") != null ?
                                                Integer.valueOf(item.get("minStock").toString()) : 0);
                                        if (item.get("purchasePrice") != null) {
                                            book.setPurchasePrice(new BigDecimal(item.get("purchasePrice").toString()));
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
                    
                    LogUtil.info("成功加载 " + bookList.size() + " 本书籍到进货选择框");
                });
            } catch (Exception e) {
                LogUtil.error("加载书籍列表失败", e);
                SwingUtilities.invokeLater(() -> {
                    DialogUtil.showErrorDialog(this, "加载书籍列表失败: " + e.getMessage());
                });
            }
        });
    }

    private void loadSuppliers() {
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.get("/suppliers/active");
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    supplierList.clear();
                    supplierComboBox.removeAllItems();
                    
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Object dataObj = result.get("data");
                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            
                            if (data != null && !data.isEmpty()) {
                                for (Map<String, Object> item : data) {
                                    try {
                                        Supplier supplier = new Supplier();
                                        if (item.get("id") != null) {
                                            supplier.setId(Long.valueOf(item.get("id").toString()));
                                        }
                                        supplier.setName(item.get("name") != null ? item.get("name").toString() : "");
                                        supplierList.add(supplier);
                                        supplierComboBox.addItem(supplier);
                                    } catch (Exception e) {
                                        LogUtil.error("解析供应商数据失败: " + item, e);
                                    }
                                }
                                // 添加空选项作为默认选择
                                supplierComboBox.insertItemAt(null, 0);
                                // 默认选择空（第一个选项）
                                supplierComboBox.setSelectedIndex(0);
                                
                                LogUtil.info("成功加载 " + supplierList.size() + " 个供应商到选择框");
                            } else {
                                LogUtil.warn("供应商列表为空，请先在系统中添加供应商");
                                // 不再弹出警告对话框，只在日志中记录
                            }
                        } else {
                            LogUtil.error("加载供应商列表失败：返回数据格式错误");
                        }
                    } else {
                        String errorMsg = result != null && result.get("message") != null ? 
                                result.get("message").toString() : "未知错误";
                        LogUtil.error("加载供应商列表失败: " + errorMsg);
                        DialogUtil.showErrorDialog(this, "加载供应商列表失败: " + errorMsg);
                    }
                });
            } catch (Exception e) {
                LogUtil.error("加载供应商列表失败", e);
                SwingUtilities.invokeLater(() -> {
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = e.getClass().getSimpleName();
                    }
                    DialogUtil.showErrorDialog(this, "加载供应商列表失败: " + errorMsg);
                });
            }
        });
    }

    private void loadPurchaseHistory() {
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 获取今日交易记录（不使用缓存，获取最新数据）
                String response = apiClient.get("/transactions/today");
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    purchaseList.clear();
                    tableModel.setRowCount(0);

                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                        for (Map<String, Object> item : data) {
                            String type = item.get("transactionType") != null ? 
                                    item.get("transactionType").toString() : "";
                            if ("PURCHASE".equals(type)) {
                                Transaction transaction = parseTransaction(item);
                                purchaseList.add(transaction);

                                String bookTitle = transaction.getBook() != null ? 
                                        transaction.getBook().getTitle() : "";
                                String supplierName = "未知";
                                if (transaction.getRelatedSupplier() != null && 
                                    transaction.getRelatedSupplier().getName() != null) {
                                    supplierName = transaction.getRelatedSupplier().getName();
                                }
                                String dateStr = transaction.getCreatedAt() != null ?
                                        transaction.getCreatedAt().format(formatter) : "";

                                tableModel.addRow(new Object[]{
                                        bookTitle,
                                        supplierName,
                                        transaction.getQuantity(),
                                        transaction.getUnitPrice(),
                                        transaction.getTotalAmount(),
                                        transaction.getNotes() != null ? transaction.getNotes() : "",
                                        dateStr
                                });
                            }
                        }
                    }
                });
            } catch (Exception e) {
                LogUtil.error("加载进货历史失败", e);
                SwingUtilities.invokeLater(() -> {
                    DialogUtil.showErrorDialog(this, "加载进货历史失败: " + e.getMessage());
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
            if (bookData.get("stockQuantity") != null) {
                book.setStockQuantity(Integer.valueOf(bookData.get("stockQuantity").toString()));
            }
            transaction.setBook(book);
        }

        // 解析供应商信息
        if (item.get("relatedSupplier") != null) {
            Map<String, Object> supplierData = (Map<String, Object>) item.get("relatedSupplier");
            Supplier supplier = new Supplier();
            supplier.setId(Long.valueOf(supplierData.get("id").toString()));
            supplier.setName(supplierData.get("name") != null ? supplierData.get("name").toString() : "");
            transaction.setRelatedSupplier(supplier);
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
