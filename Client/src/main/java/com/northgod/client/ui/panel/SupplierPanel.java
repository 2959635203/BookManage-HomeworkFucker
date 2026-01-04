package com.northgod.client.ui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.northgod.client.model.Supplier;
import com.northgod.client.service.ApiClient;
import com.northgod.client.ui.dialog.SupplierDialog;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class SupplierPanel extends JPanel {
    private final ApiClient apiClient;
    private final com.northgod.client.ui.MainFrame mainFrame;
    private JTable supplierTable;
    private DefaultTableModel tableModel;
    private final List<Supplier> supplierList;
    private JProgressBar loadingBar;
    private JButton refreshButton;
    private JButton searchButton;
    private JTextField searchField;

    public SupplierPanel(ApiClient apiClient, com.northgod.client.ui.MainFrame mainFrame) {
        this.apiClient = apiClient;
        this.mainFrame = mainFrame;
        this.supplierList = new ArrayList<>();
        initComponents();
        loadSuppliersAsync();
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

        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        toolbar.add(buttonPanel, BorderLayout.WEST);

        // 搜索面板
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchField = new JTextField(20);
        searchField.setToolTipText("输入供应商名称、联系人、电话进行搜索");

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

        // 供应商表格（移除ID列和状态列）
        String[] columnNames = {"供应商名称", "联系人", "联系电话", "邮箱", "地址", "信用评级", "付款条款", "备注"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };

        supplierTable = new JTable(tableModel);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        supplierTable.setRowHeight(25);
        supplierTable.getTableHeader().setReorderingAllowed(false);
        supplierTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // 设置所有单元格居中对齐
        supplierTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // 设置列宽（共8列：索引0-7）
        supplierTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        supplierTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        supplierTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        supplierTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        supplierTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        supplierTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        supplierTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        supplierTable.getColumnModel().getColumn(7).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(supplierTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("供应商列表"));
        add(scrollPane, BorderLayout.CENTER);

        // 事件处理
        refreshButton.addActionListener(e -> loadSuppliersAsync());
        
        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> showDeleteDialog());

        searchButton.addActionListener(e -> searchSuppliers());
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadSuppliersAsync();
        });

        // 回车键搜索
        searchField.addActionListener(e -> searchSuppliers());
    }

    public void loadSuppliersAsync() {
        refreshButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在加载供应商列表...");

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.get("/suppliers?size=100");
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    if (isSuccessResponse(result)) {
                        Object dataObj = result.get("data");
                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            updateSupplierTable(data);
                            loadingBar.setString("加载完成，共 " + data.size() + " 条记录");
                        } else {
                            loadingBar.setString("数据格式错误");
                        }
                    } else {
                        loadingBar.setString("加载失败");
                        String message = result != null && result.get("message") != null ?
                                result.get("message").toString() : "未知错误";
                        DialogUtil.showErrorDialog(this, "加载供应商列表失败: " + message);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    loadingBar.setString("加载失败");
                    DialogUtil.showErrorDialog(this, "加载供应商列表失败: " + e.getMessage());
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

    private void updateSupplierTable(List<Map<String, Object>> data) {
        supplierList.clear();
        tableModel.setRowCount(0);

        for (Map<String, Object> item : data) {
            try {
                Supplier supplier = parseSupplier(item);
                supplierList.add(supplier);

                Object[] row = {
                        supplier.getName(),
                        supplier.getContactPerson() != null ? supplier.getContactPerson() : "",
                        supplier.getContactPhone() != null ? supplier.getContactPhone() : "",
                        supplier.getEmail() != null ? supplier.getEmail() : "",
                        supplier.getAddress() != null ? supplier.getAddress() : "",
                        supplier.getCreditRating() != null ? supplier.getCreditRating() : "",
                        supplier.getPaymentTerms() != null ? supplier.getPaymentTerms() : "",
                        supplier.getNotes() != null ? supplier.getNotes() : ""
                };
                tableModel.addRow(row);
            } catch (Exception e) {
                LogUtil.error("解析供应商数据失败: " + item, e);
            }
        }
    }

    /**
     * 安全地检查响应是否成功，支持Boolean和String类型
     */
    private boolean isSuccessResponse(Map<String, Object> result) {
        if (result == null) {
            return false;
        }
        Object successObj = result.get("success");
        if (successObj == null) {
            return false;
        }
        if (successObj instanceof Boolean boolValue) {
            return boolValue;
        }
        if (successObj instanceof String strValue) {
            return Boolean.parseBoolean(strValue);
        }
        return Boolean.parseBoolean(successObj.toString());
    }

    private Supplier parseSupplier(Map<String, Object> item) {
        Supplier supplier = new Supplier();
        if (item.get("id") != null) {
            supplier.setId(Long.valueOf(item.get("id").toString()));
        }
        supplier.setName(item.get("name") != null ? item.get("name").toString() : "");
        supplier.setContactPhone(item.get("contactPhone") != null ? item.get("contactPhone").toString() : null);
        supplier.setEmail(item.get("email") != null ? item.get("email").toString() : null);
        supplier.setAddress(item.get("address") != null ? item.get("address").toString() : null);
        supplier.setContactPerson(item.get("contactPerson") != null ? item.get("contactPerson").toString() : null);
        supplier.setCreditRating(item.get("creditRating") != null ? item.get("creditRating").toString() : null);
        supplier.setPaymentTerms(item.get("paymentTerms") != null ? item.get("paymentTerms").toString() : null);
        
        // 安全地处理isActive字段，支持Boolean和String两种类型
        Object isActiveObj = item.get("isActive");
        if (isActiveObj != null) {
            if (isActiveObj instanceof Boolean boolValue) {
                supplier.setIsActive(boolValue);
            } else {
                // 对于非Boolean类型，转为字符串再解析
                supplier.setIsActive(Boolean.parseBoolean(String.valueOf(isActiveObj)));
            }
        } else {
            supplier.setIsActive(true); // 默认值为true
        }
        
        supplier.setNotes(item.get("notes") != null ? item.get("notes").toString() : null);
        return supplier;
    }

    private void searchSuppliers() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            DialogUtil.showWarningDialog(this, "请输入搜索关键词");
            return;
        }

        refreshButton.setEnabled(false);
        searchButton.setEnabled(false);
        loadingBar.setVisible(true);
        loadingBar.setIndeterminate(true);
        loadingBar.setString("正在搜索: " + keyword);

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.get("/suppliers/search?keyword=" + 
                        java.net.URLEncoder.encode(keyword, "UTF-8") + "&size=100");
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    if (isSuccessResponse(result)) {
                        Object dataObj = result.get("data");
                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            if (!data.isEmpty()) {
                                updateSupplierTable(data);
                                loadingBar.setString("搜索完成，找到 " + data.size() + " 条结果");
                            } else {
                                loadingBar.setString("未找到匹配的供应商");
                                DialogUtil.showInfoDialog(this, "未找到匹配的供应商", "搜索结果");
                                loadSuppliersAsync();
                            }
                        } else {
                            loadingBar.setString("搜索失败：数据格式错误");
                            DialogUtil.showErrorDialog(this, "搜索失败: 数据格式错误");
                        }
                    } else {
                        loadingBar.setString("搜索失败");
                        String message = result != null && result.get("message") != null ?
                                result.get("message").toString() : "未知错误";
                        DialogUtil.showErrorDialog(this, "搜索失败: " + message);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    loadingBar.setString("搜索失败");
                    DialogUtil.showErrorDialog(this, "搜索供应商失败: " + e.getMessage());
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
        SupplierDialog dialog = new SupplierDialog((Frame) SwingUtilities.getWindowAncestor(this), "添加供应商", null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            ThreadPoolManager.getInstance().submitIoTask(() -> {
                try {
                    Supplier supplier = dialog.getSupplier();
                    Map<String, Object> requestData = convertSupplierToMap(supplier);
                    String response = apiClient.post("/suppliers", requestData);
                    Map<String, Object> result = JsonUtil.parseJson(response);

                    SwingUtilities.invokeLater(() -> {
                        if (isSuccessResponse(result)) {
                            DialogUtil.showSuccessDialog(this, "添加成功");
                            loadSuppliersAsync();
                            // 通知其他面板刷新供应商下拉框
                            if (mainFrame != null) {
                                mainFrame.refreshSupplierDropdowns();
                            }
                        } else {
                            String message = result != null && result.get("message") != null ?
                                    result.get("message").toString() : "未知错误";
                            DialogUtil.showErrorDialog(this, "添加失败: " + message);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        DialogUtil.showErrorDialog(this, "添加供应商失败: " + e.getMessage());
                    });
                }
            });
        }
    }

    private void showEditDialog() {
        int selectedRow = supplierTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtil.showWarningDialog(this, "请先选择要编辑的供应商");
            return;
        }

        int modelRow = supplierTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= supplierList.size()) {
            DialogUtil.showErrorDialog(this, "选择的记录无效");
            return;
        }

        Supplier supplier = supplierList.get(modelRow);
        SupplierDialog dialog = new SupplierDialog((Frame) SwingUtilities.getWindowAncestor(this), "编辑供应商", supplier);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            ThreadPoolManager.getInstance().submitIoTask(() -> {
                try {
                    Supplier updatedSupplier = dialog.getSupplier();
                    Map<String, Object> requestData = convertSupplierToMap(updatedSupplier);
                    String response = apiClient.put("/suppliers/" + supplier.getId(), requestData);
                    Map<String, Object> result = JsonUtil.parseJson(response);

                    SwingUtilities.invokeLater(() -> {
                        if (isSuccessResponse(result)) {
                            DialogUtil.showSuccessDialog(this, "更新成功");
                            loadSuppliersAsync();
                            // 通知其他面板刷新供应商下拉框
                            if (mainFrame != null) {
                                mainFrame.refreshSupplierDropdowns();
                            }
                        } else {
                            String message = result != null && result.get("message") != null ?
                                    result.get("message").toString() : "未知错误";
                            DialogUtil.showErrorDialog(this, "更新失败: " + message);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        DialogUtil.showErrorDialog(this, "更新供应商失败: " + e.getMessage());
                    });
                }
            });
        }
    }

    private void showDeleteDialog() {
        int selectedRow = supplierTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtil.showWarningDialog(this, "请先选择要删除的供应商");
            return;
        }

        int modelRow = supplierTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= supplierList.size()) {
            DialogUtil.showErrorDialog(this, "选择的记录无效");
            return;
        }

        Supplier supplier = supplierList.get(modelRow);
        int result = JOptionPane.showConfirmDialog(this,
                "确定要删除供应商 \"" + supplier.getName() + "\" 吗？\n" +
                        "删除后将无法使用该供应商进行进货操作。",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            ThreadPoolManager.getInstance().submitIoTask(() -> {
                try {
                    String response = apiClient.delete("/suppliers/" + supplier.getId());
                    Map<String, Object> result2 = JsonUtil.parseJson(response);

                    SwingUtilities.invokeLater(() -> {
                        if (isSuccessResponse(result2)) {
                            DialogUtil.showSuccessDialog(this, "删除成功");
                            loadSuppliersAsync();
                            // 通知其他面板刷新供应商下拉框
                            if (mainFrame != null) {
                                mainFrame.refreshSupplierDropdowns();
                            }
                        } else {
                            String message = result2 != null && result2.get("message") != null ?
                                    result2.get("message").toString() : "未知错误";
                            DialogUtil.showErrorDialog(this, "删除失败: " + message);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        DialogUtil.showErrorDialog(this, "删除供应商失败: " + e.getMessage());
                    });
                }
            });
        }
    }

    private Map<String, Object> convertSupplierToMap(Supplier supplier) {
        Map<String, Object> map = new HashMap<>();
        if (supplier.getName() != null) {
            map.put("name", supplier.getName());
        }
        if (supplier.getContactPhone() != null && !supplier.getContactPhone().trim().isEmpty()) {
            map.put("contactPhone", supplier.getContactPhone());
        }
        if (supplier.getEmail() != null && !supplier.getEmail().trim().isEmpty()) {
            map.put("email", supplier.getEmail());
        }
        if (supplier.getAddress() != null && !supplier.getAddress().trim().isEmpty()) {
            map.put("address", supplier.getAddress());
        }
        if (supplier.getContactPerson() != null && !supplier.getContactPerson().trim().isEmpty()) {
            map.put("contactPerson", supplier.getContactPerson());
        }
        if (supplier.getCreditRating() != null && !supplier.getCreditRating().trim().isEmpty()) {
            map.put("creditRating", supplier.getCreditRating());
        }
        if (supplier.getPaymentTerms() != null && !supplier.getPaymentTerms().trim().isEmpty()) {
            map.put("paymentTerms", supplier.getPaymentTerms());
        }
        if (supplier.getNotes() != null && !supplier.getNotes().trim().isEmpty()) {
            map.put("notes", supplier.getNotes());
        }
        return map;
    }
}

