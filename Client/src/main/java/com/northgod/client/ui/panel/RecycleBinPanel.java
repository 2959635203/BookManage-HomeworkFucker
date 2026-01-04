package com.northgod.client.ui.panel;

import com.northgod.client.model.Book;
import com.northgod.client.model.Supplier;
import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecycleBinPanel extends JPanel {
    private final ApiClient apiClient;
    private final com.northgod.client.ui.MainFrame mainFrame;
    
    // 书籍回收站
    private JTable bookTable;
    private DefaultTableModel bookTableModel;
    private final List<Book> bookList;
    
    // 供应商回收站
    private JTable supplierTable;
    private DefaultTableModel supplierTableModel;
    private final List<Supplier> supplierList;
    
    private JProgressBar loadingBar;
    private JButton refreshButton;
    private JTabbedPane tabbedPane;

    public RecycleBinPanel(ApiClient apiClient, com.northgod.client.ui.MainFrame mainFrame) {
        this.apiClient = apiClient;
        this.mainFrame = mainFrame;
        this.bookList = new ArrayList<>();
        this.supplierList = new ArrayList<>();
        initComponents();
        loadDeletedBooks();
        loadDeletedSuppliers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new BorderLayout(5, 5));
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        refreshButton = new JButton("刷新");
        JButton restoreButton = new JButton("恢复选中");
        JButton restoreAllButton = new JButton("全部恢复");
        
        // 搜索功能
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("搜索");
        JButton clearSearchButton = new JButton("清除");

        refreshButton.addActionListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                loadDeletedBooks();
            } else {
                loadDeletedSuppliers();
            }
        });
        
        restoreButton.addActionListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                restoreSelectedBook();
            } else {
                restoreSelectedSupplier();
            }
        });
        
        restoreAllButton.addActionListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                restoreAllBooks();
            } else {
                restoreAllSuppliers();
            }
        });
        
        // 搜索功能
        searchButton.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            if (keyword.isEmpty()) {
                DialogUtil.showWarningDialog(this, "请输入搜索关键词");
                return;
            }
            searchItems(keyword);
        });
        
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                loadDeletedBooks();
            } else {
                loadDeletedSuppliers();
            }
        });
        
        // 回车键触发搜索
        searchField.addActionListener(e -> searchButton.doClick());

        leftPanel.add(refreshButton);
        leftPanel.add(restoreButton);
        leftPanel.add(restoreAllButton);
        
        rightPanel.add(new JLabel("搜索:"));
        rightPanel.add(searchField);
        rightPanel.add(searchButton);
        rightPanel.add(clearSearchButton);
        
        toolbar.add(leftPanel, BorderLayout.WEST);
        toolbar.add(rightPanel, BorderLayout.EAST);

        // 加载进度条
        loadingBar = new JProgressBar();
        loadingBar.setVisible(false);
        loadingBar.setStringPainted(true);
        
        // 将工具栏和进度条放在一个容器中
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar, BorderLayout.CENTER);
        topPanel.add(loadingBar, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);

        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        
        // 书籍回收站
        String[] bookColumnNames = {"ISBN", "书名", "作者", "出版社", "进价", "售价", "库存", "最低库存", "删除时间"};
        bookTableModel = new DefaultTableModel(bookColumnNames, 0) {
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

        bookTable = new JTable(bookTableModel);
        bookTable.setRowHeight(25);
        bookTable.setAutoCreateRowSorter(true);
        bookTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // 设置所有单元格居中对齐
        bookTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
        
        // 确保BigDecimal类型列也居中对齐
        bookTable.setDefaultRenderer(BigDecimal.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
        
        // 确保Integer类型列也居中对齐
        bookTable.setDefaultRenderer(Integer.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // 设置列宽
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(100); // ISBN
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 书名
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 作者
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 出版社
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // 进价
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // 售价
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(50);  // 库存
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(70);  // 最低库存
        bookTable.getColumnModel().getColumn(8).setPreferredWidth(150); // 删除时间

        JScrollPane bookScrollPane = new JScrollPane(bookTable);
        tabbedPane.addTab("书籍回收站", bookScrollPane);
        
        // 供应商回收站
        String[] supplierColumnNames = {"供应商名称", "联系人", "联系电话", "邮箱", "地址", "信用评级", "付款条款", "删除时间"};
        supplierTableModel = new DefaultTableModel(supplierColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        supplierTable = new JTable(supplierTableModel);
        supplierTable.setRowHeight(25);
        supplierTable.setAutoCreateRowSorter(true);
        supplierTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // 默认按照删除时间（操作时间）降序排序
        javax.swing.RowSorter<?> supplierSorter = supplierTable.getRowSorter();
        if (supplierSorter != null) {
            java.util.List<javax.swing.RowSorter.SortKey> sortKeys = 
                new java.util.ArrayList<>();
            sortKeys.add(new javax.swing.RowSorter.SortKey(7, javax.swing.SortOrder.DESCENDING));
            supplierSorter.setSortKeys(sortKeys);
        }

        // 设置列宽
        supplierTable.getColumnModel().getColumn(0).setPreferredWidth(150); // 供应商名称
        supplierTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // 联系人
        supplierTable.getColumnModel().getColumn(2).setPreferredWidth(100); // 联系电话
        supplierTable.getColumnModel().getColumn(3).setPreferredWidth(120); // 邮箱
        supplierTable.getColumnModel().getColumn(4).setPreferredWidth(150); // 地址
        supplierTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // 信用评级
        supplierTable.getColumnModel().getColumn(6).setPreferredWidth(100); // 付款条款
        supplierTable.getColumnModel().getColumn(7).setPreferredWidth(150); // 删除时间

        JScrollPane supplierScrollPane = new JScrollPane(supplierTable);
        tabbedPane.addTab("供应商回收站", supplierScrollPane);
        
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadDeletedBooks() {
        refreshButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在加载回收站书籍...");

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 分页加载所有已删除的书籍（每页最多100条）
                List<Map<String, Object>> allBooks = new ArrayList<>();
                int page = 0;
                int size = 100;
                boolean hasMore = true;
                
                while (hasMore) {
                    String response = apiClient.get("/books/deleted?page=" + page + "&size=" + size);
                    Map<String, Object> result = JsonUtil.parseJson(response);
                    
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Object dataObj = result.get("data");
                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            
                            if (data != null && !data.isEmpty()) {
                                allBooks.addAll(data);
                                
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
                
                final List<Map<String, Object>> finalBooks = allBooks;
                SwingUtilities.invokeLater(() -> {
                    try {
                        updateBookTable(finalBooks);
                        loadingBar.setString("加载成功，共 " + bookList.size() + " 本已删除书籍");
                    } catch (Exception ex) {
                        LogUtil.error("处理回收站书籍数据时发生异常", ex);
                        loadingBar.setString("加载失败");
                        DialogUtil.showErrorDialog(this, "处理数据时发生错误: " + ex.getMessage());
                    } finally {
                        refreshButton.setEnabled(true);
                        loadingBar.setIndeterminate(false);
                        loadingBar.setVisible(false);
                    }
                });
            } catch (Exception e) {
                LogUtil.error("加载回收站书籍列表异常", e);
                SwingUtilities.invokeLater(() -> {
                    loadingBar.setString("加载失败");
                    DialogUtil.showErrorDialog(this, "加载回收站书籍失败: " + e.getMessage());
                    refreshButton.setEnabled(true);
                    loadingBar.setIndeterminate(false);
                    loadingBar.setVisible(false);
                });
            }
        });
    }

    private void updateBookTable(List<Map<String, Object>> data) {
        bookList.clear();
        bookTableModel.setRowCount(0);

        for (Map<String, Object> item : data) {
            try {
                Book book = new Book();
                
                Object idObj = item.get("id");
                if (idObj != null) {
                    book.setId(Long.valueOf(idObj.toString()));
                }
                
                book.setIsbn(getStringValue(item, "isbn"));
                book.setTitle(getStringValue(item, "title"));
                book.setAuthor(getStringValue(item, "author"));
                book.setPublisher(getStringValue(item, "publisher"));
                
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

                // 获取删除时间（updatedAt）
                String deletedTime = "";
                if (item.get("updatedAt") != null) {
                    deletedTime = item.get("updatedAt").toString();
                    if (deletedTime.length() > 19) {
                        deletedTime = deletedTime.substring(0, 19);
                    }
                }

                bookTableModel.addRow(new Object[]{
                        book.getIsbn(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getPublisher(),
                        book.getPurchasePrice(),
                        book.getSellingPrice(),
                        book.getStockQuantity(),
                        book.getMinStock(),
                        deletedTime
                });
            } catch (Exception e) {
                LogUtil.error("解析回收站书籍数据失败: " + item, e);
            }
        }
        
        LogUtil.info("成功加载 " + bookList.size() + " 本已删除书籍到回收站");
    }
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private void restoreSelectedBook() {
        int[] selectedRows = bookTable.getSelectedRows();
        if (selectedRows.length == 0) {
            DialogUtil.showWarningDialog(this, "请先选择要恢复的书籍");
            return;
        }

        boolean confirm = DialogUtil.showConfirmDialog(this,
                "确定要恢复选中的 " + selectedRows.length + " 本书籍吗？");
        if (!confirm) {
            return;
        }

        restoreBooks(selectedRows);
    }

    private void restoreAllBooks() {
        if (bookList.isEmpty()) {
            DialogUtil.showWarningDialog(this, "回收站中没有书籍");
            return;
        }

        boolean confirm = DialogUtil.showConfirmDialog(this,
                "确定要恢复回收站中的所有 " + bookList.size() + " 本书籍吗？");
        if (!confirm) {
            return;
        }

        int[] allRows = new int[bookList.size()];
        for (int i = 0; i < bookList.size(); i++) {
            allRows[i] = i;
        }
        restoreBooks(allRows);
    }

    private void restoreBooks(int[] selectedRows) {
        refreshButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在恢复书籍...");

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            int successCount = 0;
            int failCount = 0;

            for (int selectedRow : selectedRows) {
                try {
                    int modelRow = bookTable.convertRowIndexToModel(selectedRow);
                    if (modelRow < 0 || modelRow >= bookList.size()) {
                        failCount++;
                        continue;
                    }

                    Book book = bookList.get(modelRow);
                    String response = apiClient.post("/books/" + book.getId() + "/restore", null);
                    Map<String, Object> result = JsonUtil.parseJson(response);

                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    LogUtil.error("恢复书籍失败: " + selectedRow, e);
                    failCount++;
                }
            }

            final int finalSuccessCount = successCount;
            final int finalFailCount = failCount;

            SwingUtilities.invokeLater(() -> {
                loadingBar.setIndeterminate(false);
                loadingBar.setVisible(false);
                refreshButton.setEnabled(true);

                if (finalSuccessCount > 0) {
                    DialogUtil.showSuccessDialog(this, 
                            "恢复完成！\n成功: " + finalSuccessCount + " 本\n失败: " + finalFailCount + " 本");
                    loadDeletedBooks();
                    // 刷新书籍管理页面
                    if (mainFrame != null) {
                        mainFrame.refreshBookPanel();
                        mainFrame.refreshBookDropdowns();
                    }
                } else {
                    DialogUtil.showErrorDialog(this, "恢复失败: " + finalFailCount + " 本");
                }
            });
        });
    }
    
    // ========== 供应商回收站相关方法 ==========
    
    private void loadDeletedSuppliers() {
        refreshButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在加载回收站供应商...");

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 分页加载所有已删除的供应商（每页最多100条）
                List<Map<String, Object>> allSuppliers = new ArrayList<>();
                int page = 0;
                int size = 100;
                boolean hasMore = true;
                
                while (hasMore) {
                    String response = apiClient.get("/suppliers/deleted?page=" + page + "&size=" + size);
                    Map<String, Object> result = JsonUtil.parseJson(response);
                    
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Object dataObj = result.get("data");
                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            
                            if (data != null && !data.isEmpty()) {
                                allSuppliers.addAll(data);
                                
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
                
                final List<Map<String, Object>> finalSuppliers = allSuppliers;
                SwingUtilities.invokeLater(() -> {
                    try {
                        updateSupplierTable(finalSuppliers);
                        loadingBar.setString("加载成功，共 " + supplierList.size() + " 个已删除供应商");
                    } catch (Exception ex) {
                        LogUtil.error("处理回收站供应商数据时发生异常", ex);
                        loadingBar.setString("加载失败");
                        DialogUtil.showErrorDialog(this, "处理数据时发生错误: " + ex.getMessage());
                    } finally {
                        refreshButton.setEnabled(true);
                        loadingBar.setIndeterminate(false);
                        loadingBar.setVisible(false);
                    }
                });
            } catch (Exception e) {
                LogUtil.error("加载回收站供应商列表异常", e);
                SwingUtilities.invokeLater(() -> {
                    loadingBar.setString("加载失败");
                    DialogUtil.showErrorDialog(this, "加载回收站供应商失败: " + e.getMessage());
                    refreshButton.setEnabled(true);
                    loadingBar.setIndeterminate(false);
                    loadingBar.setVisible(false);
                });
            }
        });
    }
    
    private void updateSupplierTable(List<Map<String, Object>> data) {
        supplierList.clear();
        supplierTableModel.setRowCount(0);

        for (Map<String, Object> item : data) {
            try {
                Supplier supplier = new Supplier();
                
                Object idObj = item.get("id");
                if (idObj != null) {
                    supplier.setId(Long.valueOf(idObj.toString()));
                }
                
                supplier.setName(getStringValue(item, "name"));
                supplier.setContactPerson(getStringValue(item, "contactPerson"));
                supplier.setContactPhone(getStringValue(item, "contactPhone"));
                supplier.setEmail(getStringValue(item, "email"));
                supplier.setAddress(getStringValue(item, "address"));
                supplier.setCreditRating(getStringValue(item, "creditRating"));
                supplier.setPaymentTerms(getStringValue(item, "paymentTerms"));

                supplierList.add(supplier);

                // 获取删除时间（updatedAt）
                String deletedTime = "";
                if (item.get("updatedAt") != null) {
                    deletedTime = item.get("updatedAt").toString();
                    if (deletedTime.length() > 19) {
                        deletedTime = deletedTime.substring(0, 19);
                    }
                }

                supplierTableModel.addRow(new Object[]{
                        supplier.getName(),
                        supplier.getContactPerson() != null ? supplier.getContactPerson() : "",
                        supplier.getContactPhone() != null ? supplier.getContactPhone() : "",
                        supplier.getEmail() != null ? supplier.getEmail() : "",
                        supplier.getAddress() != null ? supplier.getAddress() : "",
                        supplier.getCreditRating() != null ? supplier.getCreditRating() : "",
                        supplier.getPaymentTerms() != null ? supplier.getPaymentTerms() : "",
                        deletedTime
                });
            } catch (Exception e) {
                LogUtil.error("解析回收站供应商数据失败: " + item, e);
            }
        }
        
        LogUtil.info("成功加载 " + supplierList.size() + " 个已删除供应商到回收站");
    }
    
    private void restoreSelectedSupplier() {
        int[] selectedRows = supplierTable.getSelectedRows();
        if (selectedRows.length == 0) {
            DialogUtil.showWarningDialog(this, "请先选择要恢复的供应商");
            return;
        }

        boolean confirm = DialogUtil.showConfirmDialog(this,
                "确定要恢复选中的 " + selectedRows.length + " 个供应商吗？");
        if (!confirm) {
            return;
        }

        restoreSuppliers(selectedRows);
    }

    private void restoreAllSuppliers() {
        if (supplierList.isEmpty()) {
            DialogUtil.showWarningDialog(this, "回收站中没有供应商");
            return;
        }

        boolean confirm = DialogUtil.showConfirmDialog(this,
                "确定要恢复回收站中的所有 " + supplierList.size() + " 个供应商吗？");
        if (!confirm) {
            return;
        }

        int[] allRows = new int[supplierList.size()];
        for (int i = 0; i < supplierList.size(); i++) {
            allRows[i] = i;
        }
        restoreSuppliers(allRows);
    }

    private void restoreSuppliers(int[] selectedRows) {
        refreshButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在恢复供应商...");

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            int successCount = 0;
            int failCount = 0;

            for (int selectedRow : selectedRows) {
                try {
                    int modelRow = supplierTable.convertRowIndexToModel(selectedRow);
                    if (modelRow < 0 || modelRow >= supplierList.size()) {
                        failCount++;
                        continue;
                    }

                    Supplier supplier = supplierList.get(modelRow);
                    String response = apiClient.post("/suppliers/" + supplier.getId() + "/restore", null);
                    Map<String, Object> result = JsonUtil.parseJson(response);

                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    LogUtil.error("恢复供应商失败: " + selectedRow, e);
                    failCount++;
                }
            }

            final int finalSuccessCount = successCount;
            final int finalFailCount = failCount;

            SwingUtilities.invokeLater(() -> {
                loadingBar.setIndeterminate(false);
                loadingBar.setVisible(false);
                refreshButton.setEnabled(true);

                if (finalSuccessCount > 0) {
                    DialogUtil.showSuccessDialog(this, 
                            "恢复完成！\n成功: " + finalSuccessCount + " 个\n失败: " + finalFailCount + " 个");
                    loadDeletedSuppliers();
                    // 刷新供应商管理页面
                    if (mainFrame != null) {
                        mainFrame.refreshSupplierPanel();
                    }
                } else {
                    DialogUtil.showErrorDialog(this, "恢复失败: " + finalFailCount + " 个");
                }
            });
        });
    }
    
    /**
     * 搜索回收站中的项目
     */
    private void searchItems(String keyword) {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex == 0) {
            searchBooks(keyword);
        } else {
            searchSuppliers(keyword);
        }
    }
    
    /**
     * 搜索书籍
     */
    private void searchBooks(String keyword) {
        if (bookList.isEmpty()) {
            DialogUtil.showWarningDialog(this, "没有可搜索的书籍");
            return;
        }
        
        List<Map<String, Object>> filteredBooks = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (Book book : bookList) {
            boolean match = false;
            if (book.getIsbn() != null && book.getIsbn().toLowerCase().contains(lowerKeyword)) {
                match = true;
            } else if (book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerKeyword)) {
                match = true;
            } else if (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lowerKeyword)) {
                match = true;
            } else if (book.getPublisher() != null && book.getPublisher().toLowerCase().contains(lowerKeyword)) {
                match = true;
            }
            
            if (match) {
                Map<String, Object> bookMap = new HashMap<>();
                bookMap.put("id", book.getId());
                bookMap.put("isbn", book.getIsbn());
                bookMap.put("title", book.getTitle());
                bookMap.put("author", book.getAuthor());
                bookMap.put("publisher", book.getPublisher());
                bookMap.put("purchasePrice", book.getPurchasePrice());
                bookMap.put("sellingPrice", book.getSellingPrice());
                bookMap.put("stockQuantity", book.getStockQuantity());
                bookMap.put("minStock", book.getMinStock());
                filteredBooks.add(bookMap);
            }
        }
        
        if (filteredBooks.isEmpty()) {
            DialogUtil.showInfoDialog(this, "未找到匹配的书籍", "搜索结果");
        } else {
            updateBookTable(filteredBooks);
            DialogUtil.showInfoDialog(this, "找到 " + filteredBooks.size() + " 条匹配结果", "搜索结果");
        }
    }
    
    /**
     * 搜索供应商
     */
    private void searchSuppliers(String keyword) {
        if (supplierList.isEmpty()) {
            DialogUtil.showWarningDialog(this, "没有可搜索的供应商");
            return;
        }
        
        List<Map<String, Object>> filteredSuppliers = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (Supplier supplier : supplierList) {
            boolean match = false;
            if (supplier.getName() != null && supplier.getName().toLowerCase().contains(lowerKeyword)) {
                match = true;
            } else if (supplier.getContactPerson() != null && supplier.getContactPerson().toLowerCase().contains(lowerKeyword)) {
                match = true;
            } else if (supplier.getContactPhone() != null && supplier.getContactPhone().contains(keyword)) {
                match = true;
            } else if (supplier.getEmail() != null && supplier.getEmail().toLowerCase().contains(lowerKeyword)) {
                match = true;
            } else if (supplier.getAddress() != null && supplier.getAddress().toLowerCase().contains(lowerKeyword)) {
                match = true;
            }
            
            if (match) {
                Map<String, Object> supplierMap = new HashMap<>();
                supplierMap.put("id", supplier.getId());
                supplierMap.put("name", supplier.getName());
                supplierMap.put("contactPerson", supplier.getContactPerson());
                supplierMap.put("contactPhone", supplier.getContactPhone());
                supplierMap.put("email", supplier.getEmail());
                supplierMap.put("address", supplier.getAddress());
                supplierMap.put("creditRating", supplier.getCreditRating());
                supplierMap.put("paymentTerms", supplier.getPaymentTerms());
                filteredSuppliers.add(supplierMap);
            }
        }
        
        if (filteredSuppliers.isEmpty()) {
            DialogUtil.showInfoDialog(this, "未找到匹配的供应商", "搜索结果");
        } else {
            updateSupplierTable(filteredSuppliers);
            DialogUtil.showInfoDialog(this, "找到 " + filteredSuppliers.size() + " 条匹配结果", "搜索结果");
        }
    }
}

