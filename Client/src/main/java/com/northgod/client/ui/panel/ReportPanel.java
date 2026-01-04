package com.northgod.client.ui.panel;

import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class ReportPanel extends JPanel {
    private final ApiClient apiClient;
    
    // 统计显示组件
    private JLabel totalSalesLabel;
    private JLabel totalQuantityLabel;
    private JLabel netRevenueLabel;
    private JTable rankingTable;
    private DefaultTableModel rankingTableModel;
    
    // 年月选择
    private JSpinner yearSpinner;
    private JSpinner monthSpinner;
    private JButton queryButton;
    
    public ReportPanel(ApiClient apiClient, com.northgod.client.ui.MainFrame mainFrame) {
        this.apiClient = apiClient;
        initComponents();
        // 默认加载当前月份的统计
        loadCurrentMonthStatistics();
    }
    
    /**
     * 公共方法：刷新统计报表
     */
    public void refreshStatistics() {
        loadCurrentMonthStatistics();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 顶部：查询条件
        JPanel queryPanel = createQueryPanel();
        add(queryPanel, BorderLayout.NORTH);

        // 中部：统计信息
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);

        // 上部分：月度汇总
        JPanel summaryPanel = createSummaryPanel();
        splitPane.setTopComponent(summaryPanel);

        // 下部分：销售排行
        JPanel rankingPanel = createRankingPanel();
        splitPane.setBottomComponent(rankingPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("查询条件"));

        LocalDate now = LocalDate.now();
        yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2100, 1));
        monthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));

        panel.add(new JLabel("年份:"));
        panel.add(yearSpinner);
        panel.add(new JLabel("月份:"));
        panel.add(monthSpinner);

        queryButton = new JButton("查询");
        queryButton.addActionListener(e -> queryStatistics());
        panel.add(queryButton);

        JButton currentMonthButton = new JButton("当前月份");
        currentMonthButton.addActionListener(e -> loadCurrentMonthStatistics());
        panel.add(currentMonthButton);

        return panel;
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("月度汇总统计"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // 创建大字体标签用于显示统计数据
        Font bigFont = new Font("微软雅黑", Font.BOLD, 16);
        Font labelFont = new Font("微软雅黑", Font.PLAIN, 14);

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel label1 = new JLabel("销售总额:");
        label1.setFont(labelFont);
        panel.add(label1, gbc);
        gbc.gridx = 1;
        totalSalesLabel = new JLabel("0.00 元");
        totalSalesLabel.setFont(bigFont);
        totalSalesLabel.setForeground(Color.BLUE);
        panel.add(totalSalesLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel label2 = new JLabel("销售总量:");
        label2.setFont(labelFont);
        panel.add(label2, gbc);
        gbc.gridx = 1;
        totalQuantityLabel = new JLabel("0 本");
        totalQuantityLabel.setFont(bigFont);
        totalQuantityLabel.setForeground(Color.BLUE);
        panel.add(totalQuantityLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel label3 = new JLabel("净收入:");
        label3.setFont(labelFont);
        panel.add(label3, gbc);
        gbc.gridx = 1;
        netRevenueLabel = new JLabel("0.00 元");
        netRevenueLabel.setFont(bigFont);
        netRevenueLabel.setForeground(new Color(0, 150, 0));
        panel.add(netRevenueLabel, gbc);

        return panel;
    }

    private JPanel createRankingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("销售排行榜"));

        String[] columnNames = {"排名", "书名", "作者", "销售数量", "销售金额", "平均单价"};
        rankingTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        rankingTable = new JTable(rankingTableModel);
        rankingTable.setRowHeight(25);
        rankingTable.setAutoCreateRowSorter(true);
        
        // 设置所有单元格居中对齐
        rankingTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // 设置列宽（共6列：索引0-5）
        rankingTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        rankingTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        rankingTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        rankingTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        rankingTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        rankingTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(rankingTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadCurrentMonthStatistics() {
        LocalDate now = LocalDate.now();
        yearSpinner.setValue(now.getYear());
        monthSpinner.setValue(now.getMonthValue());
        queryStatistics();
    }

    private void queryStatistics() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();

        queryButton.setEnabled(false);

        // 加载月度汇总（不使用缓存，获取最新数据）
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.get("/transactions/summary/" + year + "/" + month);
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        updateSummaryDisplay(data);
                    } else {
                        DialogUtil.showErrorDialog(this, "加载月度汇总失败");
                    }
                    queryButton.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    LogUtil.error("加载月度汇总失败", e);
                    DialogUtil.showErrorDialog(this, "加载月度汇总失败: " + e.getMessage());
                    queryButton.setEnabled(true);
                });
            }
        });

        // 加载销售排行（不使用缓存，获取最新数据）
        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                String response = apiClient.get("/transactions/ranking/" + year + "/" + month);
                Map<String, Object> result = JsonUtil.parseJson(response);

                SwingUtilities.invokeLater(() -> {
                    if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                        updateRankingDisplay(data);
                    } else {
                        DialogUtil.showErrorDialog(this, "加载销售排行失败");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    LogUtil.error("加载销售排行失败", e);
                    DialogUtil.showErrorDialog(this, "加载销售排行失败: " + e.getMessage());
                });
            }
        });
    }

    private void updateSummaryDisplay(Map<String, Object> data) {
        BigDecimal totalSales = data.get("totalSales") != null ?
                new BigDecimal(data.get("totalSales").toString()) : BigDecimal.ZERO;
        Integer saleQuantity = data.get("saleQuantity") != null ?
                Integer.valueOf(data.get("saleQuantity").toString()) : 0;
        BigDecimal netRevenue = data.get("netRevenue") != null ?
                new BigDecimal(data.get("netRevenue").toString()) : BigDecimal.ZERO;

        totalSalesLabel.setText(String.format("%.2f 元", totalSales));
        totalQuantityLabel.setText(saleQuantity + " 本");
        netRevenueLabel.setText(String.format("%.2f 元", netRevenue));
    }

    private void updateRankingDisplay(List<Map<String, Object>> data) {
        rankingTableModel.setRowCount(0);

        for (Map<String, Object> item : data) {
            Integer rank = item.get("rank") != null ?
                    Integer.valueOf(item.get("rank").toString()) : 0;
            Long bookId = item.get("bookId") != null ?
                    Long.valueOf(item.get("bookId").toString()) : 0L;
            String title = item.get("title") != null ? item.get("title").toString() : "";
            String author = item.get("author") != null ? item.get("author").toString() : "";
            Long quantity = item.get("quantity") != null ?
                    Long.valueOf(item.get("quantity").toString()) : 0L;
            BigDecimal amount = item.get("amount") != null ?
                    new BigDecimal(item.get("amount").toString()) : BigDecimal.ZERO;
            BigDecimal avgPrice = item.get("averagePrice") != null ?
                    new BigDecimal(item.get("averagePrice").toString()) : BigDecimal.ZERO;

            rankingTableModel.addRow(new Object[]{
                    rank,
                    bookId,
                    title,
                    author,
                    quantity,
                    String.format("%.2f", amount),
                    String.format("%.2f", avgPrice)
            });
        }
    }
}
