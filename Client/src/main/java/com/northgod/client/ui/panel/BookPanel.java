package com.northgod.client.ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.northgod.client.model.Book;
import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.PerformanceMonitor;
import com.northgod.client.util.ThreadPoolManager;

public class BookPanel extends JPanel {
    private final ApiClient apiClient;
    private final com.northgod.client.ui.MainFrame mainFrame;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private final List<Book> bookList;
    private JProgressBar loadingBar;
    private JButton refreshButton;
    private JButton searchButton;
    private JTextField searchField;

    public BookPanel(ApiClient apiClient, com.northgod.client.ui.MainFrame mainFrame) {
        this.apiClient = apiClient;
        this.mainFrame = mainFrame;
        this.bookList = new ArrayList<>();
        initComponents();
        loadBooksAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));

        // 操作按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        refreshButton = new JButton("刷新");
        JButton addButton = new JButton("添加");
        JButton editButton = new JButton("编辑");
        JButton deleteButton = new JButton("删除");
        JButton statsButton = new JButton("统计");

        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(statsButton);

        toolbar.add(buttonPanel, BorderLayout.WEST);

        // 搜索面板
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchField = new JTextField(20);
        searchField.setToolTipText("输入ISBN、书名、作者或出版社进行搜索");

        searchButton = new JButton("搜索");
        JButton clearSearchButton = new JButton("清除");

        searchPanel.add(new JLabel("搜索:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearSearchButton);

        toolbar.add(searchPanel, BorderLayout.EAST);

        // 加载进度条
        loadingBar = new JProgressBar();
        loadingBar.setVisible(false);
        loadingBar.setStringPainted(true);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar, BorderLayout.NORTH);
        topPanel.add(loadingBar, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // 书籍表格（移除ID列）
        String[] columnNames = {"ISBN", "书名", "作者", "出版社", "进价", "售价", "库存", "最低库存", "状态"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 4, 5 -> BigDecimal.class;
                    case 6, 7 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        bookTable = new JTable(tableModel);
        bookTable.setRowHeight(25);
        bookTable.setAutoCreateRowSorter(true);
        bookTable.setFillsViewportHeight(true);

        // 设置列宽
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(100); // ISBN
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 书名
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 作者
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 出版社
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // 进价
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // 售价
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(50);  // 库存
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(70);  // 最低库存
        bookTable.getColumnModel().getColumn(8).setPreferredWidth(80);  // 状态

        // 创建统一的渲染器，用于所有列类型（包括BigDecimal和Integer）
        javax.swing.table.TableCellRenderer unifiedRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // 设置居中对齐
                if (c instanceof JLabel) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }

                // 检查库存状态（第6列是库存，第7列是最低库存）
                Object stockObj = table.getValueAt(row, 6);
                Object minStockObj = table.getValueAt(row, 7);
                
                boolean isStockWarning = false;
                if (stockObj instanceof Integer stock && minStockObj instanceof Integer minStock) {
                    // 只有库存预警时（库存大于0但小于等于最低库存）才显示黄色
                    if (stock > 0 && stock <= minStock) {
                        isStockWarning = true;
                    }
                }

                // 只有库存预警时整行显示黄色背景，其余情况都是白底黑字
                if (isStockWarning && !isSelected) {
                    c.setBackground(Color.YELLOW);
                    c.setForeground(Color.BLACK);
                    if (c instanceof JLabel) {
                        ((JLabel) c).setOpaque(true); // 确保背景色能够显示
                    }
                } else {
                    // 正常情况：白底黑字（或选中时的默认颜色）
                    c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                    c.setForeground(isSelected ? table.getSelectionForeground() : Color.BLACK);
                    if (c instanceof JLabel) {
                        ((JLabel) c).setOpaque(true); // 确保背景色能够显示
                    }
                }

                return c;
            }
        };
        
        // 为所有类型设置渲染器，确保BigDecimal和Integer列也能正确显示
        bookTable.setDefaultRenderer(Object.class, unifiedRenderer);
        bookTable.setDefaultRenderer(BigDecimal.class, unifiedRenderer);
        bookTable.setDefaultRenderer(Integer.class, unifiedRenderer);

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);

        // 状态标签
        JLabel statusLabel = new JLabel("就绪");
        add(statusLabel, BorderLayout.SOUTH);

        // 事件监听
        refreshButton.addActionListener(e -> loadBooksAsync());
        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteBook());
        statsButton.addActionListener(e -> PerformanceMonitor.showStatsDialog(this));

        // 搜索事件
        searchButton.addActionListener(e -> searchBooksFromServer());
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadBooksAsync();
        });

        // 回车键触发搜索
        searchField.addActionListener(e -> searchBooksFromServer());
    }

    /**
     * 异步加载书籍列表（从 private 改为 public）
     */
    public void loadBooksAsync() {
        PerformanceMonitor.Timer timer = PerformanceMonitor.Timer.start("加载书籍列表");

        refreshButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在加载书籍...");

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.get("/books");
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    try {
                        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                            Object dataObj = result.get("data");
                            if (dataObj == null) {
                                loadingBar.setString("加载失败：数据为空");
                                DialogUtil.showErrorDialog(this, "加载书籍失败: 服务器返回数据为空");
                                timer.stopAndRecord();
                                return;
                            }
                            
                            // 检查数据类型
                            if (!(dataObj instanceof List)) {
                                LogUtil.error("数据类型错误，期望List，实际: " + dataObj.getClass().getName());
                                loadingBar.setString("加载失败：数据格式错误");
                                DialogUtil.showErrorDialog(this, "加载书籍失败: 数据格式错误，期望列表类型");
                                timer.stopAndRecord();
                                return;
                            }
                            
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            
                            if (data.isEmpty()) {
                                loadingBar.setString("暂无书籍数据");
                                updateBookTable(data); // 清空表格
                            } else {
                                updateBookTable(data);
                                loadingBar.setString("加载成功，共 " + bookList.size() + " 本书籍");
                            }
                        } else {
                            loadingBar.setString("加载失败");
                            String message = "未知错误";
                            if (result != null) {
                                Object msgObj = result.get("message");
                                if (msgObj != null) {
                                    message = msgObj.toString();
                                }
                            }
                            LogUtil.warn("加载书籍失败: " + message);
                            DialogUtil.showErrorDialog(this, "加载书籍失败: " + message);
                        }
                    } catch (Exception ex) {
                        LogUtil.error("处理书籍数据时发生异常", ex);
                        loadingBar.setString("加载失败");
                        DialogUtil.showDetailedErrorDialog(this, 
                                "处理书籍数据时发生错误:\n" + ex.getMessage(), "数据处理错误");
                    } finally {
                        timer.stopAndRecord();
                    }
                });
            } catch (Exception e) {
                LogUtil.error("加载书籍列表异常", e);
                SwingUtilities.invokeLater(() -> {
                    loadingBar.setString("加载失败");
                    String errorMessage = "加载书籍失败:\n" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    
                    // 提供更详细的错误信息
                    if (errorMessage.contains("Connection refused") || errorMessage.contains("连接被拒绝")) {
                        errorMessage = "无法连接到服务器\n\n" +
                                "请确认:\n" +
                                "1. 服务器已启动\n" +
                                "2. 服务器地址配置正确\n" +
                                "3. 网络连接正常";
                    } else if (errorMessage.contains("401") || errorMessage.contains("Unauthorized")) {
                        errorMessage = "认证失败\n\n请重新登录";
                    }
                    
                    DialogUtil.showDetailedErrorDialog(this, errorMessage, "网络请求失败");
                    timer.stop();
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshButton.setEnabled(true);
                    loadingBar.setIndeterminate(false);
                    loadingBar.setVisible(false);
                });
            }
        });
    }

    private void updateBookTable(List<Map<String, Object>> data) {
        bookList.clear();
        tableModel.setRowCount(0);

        for (Map<String, Object> item : data) {
            try {
                Book book = new Book();
                
                // 安全地获取ID
                Object idObj = item.get("id");
                if (idObj != null) {
                    book.setId(Long.valueOf(idObj.toString()));
                }
                
                // 安全地获取其他字段
                book.setIsbn(getStringValue(item, "isbn"));
                book.setTitle(getStringValue(item, "title"));
                book.setAuthor(getStringValue(item, "author"));
                book.setPublisher(getStringValue(item, "publisher"));
                
                // 安全地获取价格
                Object purchasePriceObj = item.get("purchasePrice");
                if (purchasePriceObj != null) {
                    book.setPurchasePrice(new BigDecimal(purchasePriceObj.toString()));
                } else {
                    book.setPurchasePrice(BigDecimal.ZERO);
                }
                
                Object sellingPriceObj = item.get("sellingPrice");
                if (sellingPriceObj != null) {
                    book.setSellingPrice(new BigDecimal(sellingPriceObj.toString()));
                } else {
                    book.setSellingPrice(BigDecimal.ZERO);
                }
                
                // 安全地获取库存
                Object stockQuantityObj = item.get("stockQuantity");
                if (stockQuantityObj != null) {
                    book.setStockQuantity(Integer.valueOf(stockQuantityObj.toString()));
                } else {
                    book.setStockQuantity(0);
                }
                
                Object minStockObj = item.get("minStock");
                if (minStockObj != null) {
                    book.setMinStock(Integer.valueOf(minStockObj.toString()));
                } else {
                    book.setMinStock(0);
                }

                bookList.add(book);

                // 根据库存状态设置状态列
                String status = "正常";
                if (book.getStockQuantity() <= 0) {
                    status = "缺货";
                } else if (book.getMinStock() != null && book.getStockQuantity() <= book.getMinStock()) {
                    status = "库存预警";
                }

                tableModel.addRow(new Object[]{
                        book.getIsbn(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getPublisher(),
                        book.getPurchasePrice(),
                        book.getSellingPrice(),
                        book.getStockQuantity(),
                        book.getMinStock(),
                        status
                });
            } catch (Exception e) {
                LogUtil.error("解析书籍数据失败: " + item, e);
                // 继续处理下一条记录，不中断整个加载过程
            }
        }
        
        LogUtil.info("成功加载 " + bookList.size() + " 本书籍到表格");
    }
    
    /**
     * 安全地获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private void searchBooksFromServer() {
        String keyword = searchField.getText().trim();

        if (keyword.isEmpty()) {
            DialogUtil.showWarningDialog(this, "请输入搜索关键词");
            searchField.requestFocus();
            return;
        }

        PerformanceMonitor.Timer timer = PerformanceMonitor.Timer.start("搜索书籍");

        refreshButton.setEnabled(false);
        searchButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在搜索: " + keyword);

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.searchBooks(keyword);
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Object dataObj = result.get("data");
                        if (dataObj != null && dataObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                                if (!data.isEmpty()) {
                                    updateBookTable(data);
                                    loadingBar.setString("搜索完成，找到 " + data.size() + " 条结果");
                                } else {
                                    loadingBar.setString("未找到匹配的书籍");
                                    DialogUtil.showInfoDialog(this, "未找到匹配的书籍", "搜索结果");
                                    // 如果没有结果，重新加载所有书籍
                                    loadBooksAsync();
                                }
                            } else {
                                loadingBar.setString("搜索失败：数据格式错误");
                                DialogUtil.showErrorDialog(this, "搜索失败: 数据格式错误");
                            }
                        } else {
                            loadingBar.setString("搜索失败");
                            String message = "未知错误";
                            if (result != null) {
                                Object msgObj = result.get("message");
                                if (msgObj != null) {
                                    message = msgObj.toString();
                                }
                            }
                            DialogUtil.showErrorDialog(this, "搜索失败: " + message);
                        }
                        timer.stopAndRecord();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    loadingBar.setString("搜索失败");
                    String errorMessage = "搜索书籍失败:\n" + e.getMessage();
                    DialogUtil.showDetailedErrorDialog(this, errorMessage, "搜索失败");
                    timer.stop();
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshButton.setEnabled(true);
                    searchButton.setEnabled(true);
                    loadingBar.setIndeterminate(false);
                    loadingBar.setVisible(false);
                });
            }
        });
    }

    private void showAddDialog() {
        SwingUtilities.invokeLater(() -> {
            BookDialog dialog = new BookDialog((Frame) SwingUtilities.getWindowAncestor(this), "添加书籍", null);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                ThreadPoolManager.getInstance().submitIoTask(() -> {
                    try {
                        Book book = dialog.getBook();
                        String response = apiClient.post("/books", book);
                        Map<String, Object> result = JsonUtil.parseJson(response);

                        SwingUtilities.invokeLater(() -> {
                            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                                DialogUtil.showSuccessDialog(this, "添加成功");
                                loadBooksAsync();
                                // 通知其他面板刷新书籍下拉框
                                if (mainFrame != null) {
                                    mainFrame.refreshBookDropdowns();
                                }
                            } else {
                                String message = result != null && result.get("message") != null ?
                                        result.get("message").toString() : "未知错误";
                                
                                // ISBN重复时采用更直观的错误警告模式
                                if (message.contains("ISBN") || message.contains("isbn") || 
                                    message.contains("重复") || message.contains("已存在") ||
                                    message.contains("duplicate") || message.contains("exists")) {
                                    JOptionPane.showMessageDialog(this,
                                        "ISBN重复警告\n\n" +
                                        "您输入的ISBN: " + dialog.getBook().getIsbn() + "\n" +
                                        "该ISBN已存在于系统中！\n\n" +
                                        "请检查：\n" +
                                        "- 是否输入了错误的ISBN\n" +
                                        "- 该书籍是否已经添加过\n" +
                                        "- 是否要编辑现有书籍\n\n" +
                                        "提示：每个ISBN只能对应一本书籍",
                                        "ISBN重复", JOptionPane.WARNING_MESSAGE);
                                } else {
                                    DialogUtil.showErrorDialog(this, "添加失败: " + message);
                                }
                            }
                        });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            String errorMessage = e.getMessage();
                            // 检查是否是ISBN重复错误
                            if (errorMessage != null && (
                                errorMessage.contains("ISBN") || 
                                errorMessage.contains("isbn") || 
                                errorMessage.contains("重复") || 
                                errorMessage.contains("已存在") ||
                                errorMessage.contains("duplicate") || 
                                errorMessage.contains("exists") ||
                                errorMessage.contains("idx_book_isbn") ||
                                errorMessage.contains("duplicate key") ||
                                errorMessage.contains("unique constraint"))) {
                                // 提取ISBN值
                                String isbn = dialog.getBook() != null ? dialog.getBook().getIsbn() : "未知";
                                if (errorMessage.contains("isbn)=(")) {
                                    try {
                                        int start = errorMessage.indexOf("isbn)=(") + 7;
                                        int end = errorMessage.indexOf(")", start);
                                        if (end > start) {
                                            isbn = errorMessage.substring(start, end);
                                        }
                                    } catch (Exception ex) {
                                        // 使用默认值
                                    }
                                }
                                
                                JOptionPane.showMessageDialog(this,
                                    "ISBN重复警告\n\n" +
                                    "您输入的ISBN: " + isbn + "\n" +
                                    "该ISBN已存在于系统中！\n\n" +
                                    "请检查：\n" +
                                    "- 是否输入了错误的ISBN\n" +
                                    "- 该书籍是否已经添加过\n" +
                                    "- 是否要编辑现有书籍\n\n" +
                                    "提示：每个ISBN只能对应一本书籍",
                                    "ISBN重复", JOptionPane.WARNING_MESSAGE);
                            } else {
                                String errorMsg = "添加书籍失败:\n" + errorMessage;
                                DialogUtil.showDetailedErrorDialog(this, errorMsg, "添加失败");
                            }
                        });
                    }
                });
            }
        });
    }

    private void showEditDialog() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtil.showWarningDialog(this, "请先选择要编辑的书籍");
            return;
        }

        // 转换视图行索引为模型索引
        int modelRow = bookTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= bookList.size()) {
            DialogUtil.showErrorDialog(this, "选择的书籍无效");
            return;
        }

        Book bookToEdit = bookList.get(modelRow);

        SwingUtilities.invokeLater(() -> {
            BookDialog dialog = new BookDialog((Frame) SwingUtilities.getWindowAncestor(this), "编辑书籍", bookToEdit);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                ThreadPoolManager.getInstance().submitIoTask(() -> {
                    try {
                        Book updatedBook = dialog.getBook();
                        updatedBook.setId(bookToEdit.getId()); // 保持原有ID

                        String response = apiClient.put("/books/" + bookToEdit.getId(), updatedBook);
                        Map<String, Object> result = JsonUtil.parseJson(response);

                        SwingUtilities.invokeLater(() -> {
                            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                                DialogUtil.showSuccessDialog(this, "编辑成功");
                                loadBooksAsync();
                                // 通知其他面板刷新书籍下拉框
                                if (mainFrame != null) {
                                    mainFrame.refreshBookDropdowns();
                                }
                            } else {
                                String message = result != null && result.get("message") != null ?
                                        result.get("message").toString() : "未知错误";
                                DialogUtil.showErrorDialog(this, "编辑失败: " + message);
                            }
                        });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            String errorMessage = "编辑书籍失败:\n" + e.getMessage();
                            DialogUtil.showDetailedErrorDialog(this, errorMessage, "编辑失败");
                        });
                    }
                });
            }
        });
    }

    private void deleteBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtil.showWarningDialog(this, "请先选择要删除的书籍");
            return;
        }

        // 转换视图行索引为模型索引
        int modelRow = bookTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= bookList.size()) {
            DialogUtil.showErrorDialog(this, "选择的书籍无效");
            return;
        }

        Book bookToDelete = bookList.get(modelRow);

        // 确认对话框
        boolean confirm = DialogUtil.showConfirmDialog(this,
                "确定要删除书籍《" + bookToDelete.getTitle() + "》吗？\n\n" +
                        "ISBN: " + bookToDelete.getIsbn() + "\n" +
                        "作者: " + bookToDelete.getAuthor() + "\n" +
                        "此操作不可撤销！");

        if (!confirm) {
            return;
        }

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.delete("/books/" + bookToDelete.getId());
                Map<String, Object> result = JsonUtil.parseJson(response);

                        SwingUtilities.invokeLater(() -> {
                            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                                DialogUtil.showSuccessDialog(this, "删除成功");
                                loadBooksAsync();
                                // 通知其他面板刷新书籍下拉框
                                if (mainFrame != null) {
                                    mainFrame.refreshBookDropdowns();
                                }
                            } else {
                        String message = result != null && result.get("message") != null ?
                                result.get("message").toString() : "未知错误";
                        DialogUtil.showErrorDialog(this, "删除失败: " + message);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    String errorMessage = "删除书籍失败:\n" + e.getMessage();
                    DialogUtil.showDetailedErrorDialog(this, errorMessage, "删除失败");
                });
            }
        });
    }
}