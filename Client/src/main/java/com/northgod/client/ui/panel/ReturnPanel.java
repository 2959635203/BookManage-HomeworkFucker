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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.northgod.client.model.Book;
import com.northgod.client.model.Transaction;
import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class ReturnPanel extends JPanel {
    private final ApiClient apiClient;
    private JTable returnTable;
    private JTable saleHistoryTable;
    private DefaultTableModel returnTableModel;
    private DefaultTableModel saleHistoryTableModel;
    private final List<Transaction> returnList;
    private final List<Transaction> saleHistoryList;
    
    // 退货表单组件
    private JComboBox<Transaction> saleComboBox;
    private JTextField quantityField;
    private JTextField unitPriceField;
    private JTextField totalAmountField;
    private JTextField notesField;
    private JLabel originalSaleInfoLabel;

    private final com.northgod.client.ui.MainFrame mainFrame;
    
    public ReturnPanel(ApiClient apiClient, com.northgod.client.ui.MainFrame mainFrame) {
        this.apiClient = apiClient;
        this.mainFrame = mainFrame;
        this.returnList = new ArrayList<>();
        this.saleHistoryList = new ArrayList<>();
        initComponents();
        loadReturnHistory();
        loadSaleHistory();
    }
    
    /**
     * 公共方法：刷新销售历史记录
     */
    public void refreshSaleHistory() {
        loadSaleHistory();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 顶部：退货表单
        JPanel formPanel = createReturnForm();
        add(formPanel, BorderLayout.NORTH);

        // 中部：分为两部分 - 历史销售记录和退货记录
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);
        
        // 上部分：历史销售记录（用于选择退货的原始销售）
        JPanel saleHistoryPanel = createSaleHistoryTable();
        splitPane.setTopComponent(saleHistoryPanel);
        
        // 下部分：退货记录
        JPanel returnPanel = createReturnTable();
        splitPane.setBottomComponent(returnPanel);
        
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createReturnForm() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("新增退货"));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 选择原销售记录
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("选择原销售记录:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        saleComboBox = new JComboBox<>();
        // 设置下拉框大小，使其能够显示完整的数据
        saleComboBox.setPreferredSize(new Dimension(400, 25));
        saleComboBox.setMaximumRowCount(20); // 显示更多选项
        saleComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean hasFocus) {
                if (value instanceof Transaction transaction) {
                    String bookTitle = transaction.getBook() != null ? transaction.getBook().getTitle() : "未知";
                    String date = transaction.getCreatedAt() != null ?
                            transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
                    return super.getListCellRendererComponent(list,
                            bookTitle + " | 数量:" + transaction.getQuantity() + " | 日期:" + date,
                            index, isSelected, hasFocus);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            }
        });
        saleComboBox.addActionListener(e -> updateOriginalSaleInfo());
        form.add(saleComboBox, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("原销售信息:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        originalSaleInfoLabel = new JLabel("");
        form.add(originalSaleInfoLabel, gbc);
        gbc.gridwidth = 1;

        // 退货数量（不可编辑，根据订单自动填充）
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("退货数量:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        quantityField = new JTextField(15);
        quantityField.setEditable(false);
        quantityField.setBackground(Color.LIGHT_GRAY);
        quantityField.addActionListener(e -> calculateTotal());
        form.add(quantityField, gbc);
        gbc.gridwidth = 1;

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

        panel.add(form, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("提交退货");
        JButton resetButton = new JButton("重置");
        JButton printButton = new JButton("打印退货单");

        submitButton.addActionListener(e -> submitReturn());
        resetButton.addActionListener(e -> resetForm());
        printButton.addActionListener(e -> printReturnOrder());

        buttonPanel.add(submitButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(printButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSaleHistoryTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("历史销售记录（选择退货的原始销售）"));

        String[] columnNames = {"书籍", "数量", "单价", "总金额", "日期"};
        saleHistoryTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        saleHistoryTable = new JTable(saleHistoryTableModel);
        saleHistoryTable.setRowHeight(25);
        saleHistoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 设置所有单元格居中对齐
        saleHistoryTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
        
        saleHistoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = saleHistoryTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = saleHistoryTable.convertRowIndexToModel(selectedRow);
                    if (modelRow >= 0 && modelRow < saleHistoryList.size()) {
                        Transaction selected = saleHistoryList.get(modelRow);
                        // 更新下拉框选择
                        saleComboBox.setSelectedItem(selected);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(saleHistoryTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新销售记录");
        refreshButton.addActionListener(e -> loadSaleHistory());
        toolbar.add(refreshButton);
        panel.add(toolbar, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createReturnTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("退货历史记录"));

        String[] columnNames = {"书籍", "数量", "单价", "总金额", "备注", "日期"};
        returnTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        returnTable = new JTable(returnTableModel);
        returnTable.setRowHeight(25);
        returnTable.setAutoCreateRowSorter(true);
        
        // 设置所有单元格居中对齐
        returnTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(returnTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新退货记录");
        refreshButton.addActionListener(e -> loadReturnHistory());
        toolbar.add(refreshButton);
        panel.add(toolbar, BorderLayout.NORTH);

        return panel;
    }

    private void updateOriginalSaleInfo() {
        Transaction selected = (Transaction) saleComboBox.getSelectedItem();
        if (selected != null) {
            String bookTitle = selected.getBook() != null ? selected.getBook().getTitle() : "未知";
            originalSaleInfoLabel.setText(String.format("书籍: %s | 原销售数量: %d | 原单价: %s 元",
                    bookTitle, selected.getQuantity(), selected.getUnitPrice()));
            unitPriceField.setText(selected.getUnitPrice().toString());
            // 写死退货数量为原订单数量，不可修改
            quantityField.setText(String.valueOf(selected.getQuantity()));
            calculateTotal();
        } else {
            originalSaleInfoLabel.setText("");
            quantityField.setText("");
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
            } else {
                totalAmountField.setText("");
            }
        } catch (NumberFormatException e) {
            totalAmountField.setText("");
        }
    }

    private void submitReturn() {
        Transaction selectedSale = (Transaction) saleComboBox.getSelectedItem();
        
        if (selectedSale == null) {
            DialogUtil.showWarningDialog(this, "请选择原销售记录");
            return;
        }

        try {
            // 退货数量写死为原订单数量，不需要验证
            int quantity = selectedSale.getQuantity();
            BigDecimal unitPrice = new BigDecimal(unitPriceField.getText().trim());
            String notes = notesField.getText().trim();

            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                DialogUtil.showWarningDialog(this, "单价必须大于0");
                return;
            }

            // 创建请求数据
            Map<String, Object> requestData = new java.util.HashMap<>();
            requestData.put("book", Map.of("id", selectedSale.getBook().getId()));
            requestData.put("transactionType", "RETURN");
            requestData.put("quantity", quantity);
            requestData.put("unitPrice", unitPrice);
            requestData.put("notes", notes);
            requestData.put("relatedTransactionId", selectedSale.getId());

            ThreadPoolManager.getInstance().submitIoTask(() -> {
                try {
                    String response = apiClient.post("/transactions/return", requestData);
                    Map<String, Object> result = JsonUtil.parseJson(response);

                    SwingUtilities.invokeLater(() -> {
                        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                            DialogUtil.showSuccessDialog(this, "退货成功！");
                            resetForm();
                            loadReturnHistory();
                            loadSaleHistory(); // 刷新销售记录
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
                            DialogUtil.showErrorDialog(this, "退货失败: " + message);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        LogUtil.error("提交退货失败", e);
                        DialogUtil.showErrorDialog(this, "提交退货失败: " + e.getMessage());
                    });
                }
            });
        } catch (NumberFormatException e) {
            DialogUtil.showWarningDialog(this, "请输入有效的数字");
        }
    }

    private void resetForm() {
        saleComboBox.setSelectedIndex(-1);
        quantityField.setText("");
        unitPriceField.setText("");
        totalAmountField.setText("");
        notesField.setText("");
        originalSaleInfoLabel.setText("");
    }

    private void printReturnOrder() {
        int selectedRow = returnTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtil.showWarningDialog(this, "请先选择要打印的退货记录");
            return;
        }

        int modelRow = returnTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= returnList.size()) {
            DialogUtil.showErrorDialog(this, "选择的记录无效");
            return;
        }

        Transaction transaction = returnList.get(modelRow);
        printTransaction(transaction, "退货单");
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
            if (transaction.getRelatedTransactionId() != null) {
                content.append("原销售单号: ").append(transaction.getRelatedTransactionId()).append("\n");
            }
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

    private void loadSaleHistory() {
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 获取今日的交易记录（包括销售和退货）
                String response = apiClient.get("/transactions/today");
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    saleHistoryList.clear();
                    saleHistoryTableModel.setRowCount(0);
                    saleComboBox.removeAllItems();

                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Object dataObj = result.get("data");
                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                            // 收集所有已退货的订单ID
                            Set<Long> returnedSaleIds = new HashSet<>();
                            if (data != null && !data.isEmpty()) {
                                for (Map<String, Object> item : data) {
                                    String type = item.get("transactionType") != null ? 
                                            item.get("transactionType").toString() : "";
                                    if ("RETURN".equals(type)) {
                                        Object relatedIdObj = item.get("relatedTransactionId");
                                        if (relatedIdObj != null) {
                                            try {
                                                Long relatedId = Long.valueOf(relatedIdObj.toString());
                                                returnedSaleIds.add(relatedId);
                                                LogUtil.debug("发现已退货订单ID: " + relatedId);
                                            } catch (Exception e) {
                                                LogUtil.error("解析退货关联订单ID失败: " + relatedIdObj, e);
                                            }
                                        }
                                    }
                                }
                            }
                            LogUtil.info("共收集到 " + returnedSaleIds.size() + " 个已退货的订单ID");

                            if (data != null && !data.isEmpty()) {
                                for (Map<String, Object> item : data) {
                                    try {
                                        String type = item.get("transactionType") != null ? 
                                                item.get("transactionType").toString() : "";
                                        if ("SALE".equals(type)) {
                                            Transaction transaction = parseTransaction(item);
                                            
                                            // 排除已经退货的订单
                                            if (returnedSaleIds.contains(transaction.getId())) {
                                                LogUtil.debug("排除已退货订单: " + transaction.getId());
                                                continue;
                                            }
                                            
                                            saleHistoryList.add(transaction);
                                            // 添加到下拉框
                                            saleComboBox.addItem(transaction);

                                            String bookTitle = transaction.getBook() != null ? 
                                                    transaction.getBook().getTitle() : "";
                                            String dateStr = transaction.getCreatedAt() != null ?
                                                    transaction.getCreatedAt().format(formatter) : "";

                                            saleHistoryTableModel.addRow(new Object[]{
                                                    bookTitle,
                                                    transaction.getQuantity(),
                                                    transaction.getUnitPrice(),
                                                    transaction.getTotalAmount(),
                                                    dateStr
                                            });
                                        }
                                    } catch (Exception e) {
                                        LogUtil.error("解析销售记录失败: " + item, e);
                                    }
                                }
                                // 添加空选项作为默认选择
                                saleComboBox.insertItemAt(null, 0);
                                // 默认选择空（第一个选项）
                                saleComboBox.setSelectedIndex(0);
                                
                                LogUtil.info("成功加载 " + saleHistoryList.size() + " 条销售记录到退货选择框（已排除已退货订单）");
                            } else {
                                LogUtil.warn("今日销售记录为空");
                            }
                        }
                    } else {
                        LogUtil.error("加载销售历史失败: " + (result != null ? result.get("message") : "未知错误"));
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

    private void loadReturnHistory() {
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 获取今日交易记录（不使用缓存，获取最新数据）
                String response = apiClient.get("/transactions/today"); // 修正参数
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    returnList.clear();
                    returnTableModel.setRowCount(0);

                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                        for (Map<String, Object> item : data) {
                            String type = item.get("transactionType") != null ? 
                                    item.get("transactionType").toString() : "";
                            if ("RETURN".equals(type)) {
                                Transaction transaction = parseTransaction(item);
                                returnList.add(transaction);

                                String bookTitle = transaction.getBook() != null ? 
                                        transaction.getBook().getTitle() : "";
                                String dateStr = transaction.getCreatedAt() != null ?
                                        transaction.getCreatedAt().format(formatter) : "";

                                returnTableModel.addRow(new Object[]{
                                        bookTitle,
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
                LogUtil.error("加载退货历史失败", e);
                SwingUtilities.invokeLater(() -> {
                    DialogUtil.showErrorDialog(this, "加载退货历史失败: " + e.getMessage());
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
        
        if (item.get("relatedTransactionId") != null) {
            transaction.setRelatedTransactionId(Long.valueOf(item.get("relatedTransactionId").toString()));
        }

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
