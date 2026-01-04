package com.northgod.client.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.northgod.client.service.ApiClient;
import com.northgod.client.ui.panel.BookPanel;
import com.northgod.client.ui.panel.PurchasePanel;
import com.northgod.client.ui.panel.RecycleBinPanel;
import com.northgod.client.ui.panel.ReportPanel;
import com.northgod.client.ui.panel.ReturnPanel;
import com.northgod.client.ui.panel.SalePanel;
import com.northgod.client.ui.panel.SupplierPanel;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class MainFrame extends JFrame {
    private final ApiClient apiClient;
    private final Map<String, Object> userInfo;
    private JTabbedPane tabbedPane;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // 线程池管理器
    private final ThreadPoolManager threadPoolManager;
    
    // 保存面板引用，用于面板间通信
    private BookPanel bookPanel;
    private SupplierPanel supplierPanel;
    private PurchasePanel purchasePanel;
    private SalePanel salePanel;
    private ReturnPanel returnPanel;
    private ReportPanel reportPanel;
    private com.northgod.client.ui.panel.RecycleBinPanel recycleBinPanel;

    public MainFrame(ApiClient apiClient, Map<String, Object> userInfo) {
        this.apiClient = apiClient;
        this.userInfo = userInfo;
        this.threadPoolManager = ThreadPoolManager.getInstance();

        initComponents();
        preloadTabs();
    }

    private void initComponents() {
        setTitle("图书销售管理系统 - " + userInfo.get("fullName"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 设置应用图标
        try {
            java.net.URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                setIconImage(Toolkit.getDefaultToolkit().getImage(iconUrl));
            } else {
                LogUtil.warn("图标文件不存在: /icon.png");
            }
        } catch (Exception e) {
            LogUtil.warn("图标加载失败: " + e.getMessage());
        }

        // 创建菜单栏
        createMenuBar();

        // 创建状态栏
        createStatusBar();

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 创建进度条（初始隐藏）
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        mainPanel.add(progressBar, BorderLayout.NORTH);

        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> {
            // 选项卡切换时更新状态
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                updateStatus("切换到: " + tabbedPane.getTitleAt(selectedIndex));
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // 添加窗口监听器
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem threadPoolStatusItem = new JMenuItem("线程池状态");
        JMenuItem exitItem = new JMenuItem("退出");

        threadPoolStatusItem.addActionListener(e -> showThreadPoolStatus());
        exitItem.addActionListener(e -> {
            cleanup();
            System.exit(0);
        });

        fileMenu.add(threadPoolStatusItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 视图菜单
        JMenu viewMenu = new JMenu("视图");
        JCheckBoxMenuItem showProgressItem = new JCheckBoxMenuItem("显示进度条");
        showProgressItem.setSelected(false);
        showProgressItem.addActionListener(e ->
                progressBar.setVisible(showProgressItem.isSelected()));

        viewMenu.add(showProgressItem);

        // 系统菜单
        JMenu systemMenu = new JMenu("系统");
        JMenuItem userInfoItem = new JMenuItem("用户信息");
        JMenuItem performanceItem = new JMenuItem("性能监控");
        JMenuItem logoutItem = new JMenuItem("登出");
        JMenuItem aboutItem = new JMenuItem("关于");

        userInfoItem.addActionListener(e -> showUserInfo());
        performanceItem.addActionListener(e -> showPerformanceInfo());
        logoutItem.addActionListener(e -> logout());
        aboutItem.addActionListener(e -> showAboutDialog());

        systemMenu.add(userInfoItem);
        systemMenu.add(performanceItem);
        systemMenu.addSeparator();
        systemMenu.add(logoutItem);
        systemMenu.addSeparator();
        systemMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(systemMenu);

        setJMenuBar(menuBar);
    }

    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel("就绪");
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JLabel userLabel = new JLabel("用户: " + userInfo.get("fullName") +
                " (" + userInfo.get("role") + ")");
        statusPanel.add(userLabel, BorderLayout.EAST);

        add(statusPanel, BorderLayout.SOUTH);
    }

    private void preloadTabs() {
        // 使用SwingWorker预加载选项卡
        MainFrame mainFrameRef = this;
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish("正在加载界面...");

                // 在主线程中加载第一个选项卡
                SwingUtilities.invokeLater(() -> {
                    try {
                        bookPanel = new BookPanel(apiClient, mainFrameRef);
                        tabbedPane.addTab("书籍管理", bookPanel);
                    } catch (Exception e) {
                        LogUtil.error("创建书籍管理面板失败", e);
                        updateStatus("书籍管理面板加载失败: " + e.getMessage());
                    }
                });

                // 预加载其他选项卡（使用自定义线程池）
                threadPoolManager.submitIoTask(() -> {
                    try {
                        // 在后台线程中创建其他面板，每个面板单独处理异常
                        // 供应商管理
                        try {
                            supplierPanel = new SupplierPanel(apiClient, mainFrameRef);
                            SwingUtilities.invokeLater(() -> {
                                tabbedPane.addTab("供应商管理", supplierPanel);
                                publish("供应商管理已加载");
                            });
                        } catch (Exception e) {
                            LogUtil.error("创建供应商管理面板失败", e);
                            SwingUtilities.invokeLater(() -> {
                                updateStatus("供应商管理面板加载失败: " + e.getMessage());
                            });
                        }

                        // 进货管理
                        try {
                            purchasePanel = new PurchasePanel(apiClient, mainFrameRef);
                            SwingUtilities.invokeLater(() -> {
                                tabbedPane.addTab("进货管理", purchasePanel);
                                publish("进货管理已加载");
                            });
                        } catch (Exception e) {
                            LogUtil.error("创建进货管理面板失败", e);
                            SwingUtilities.invokeLater(() -> {
                                updateStatus("进货管理面板加载失败: " + e.getMessage());
                            });
                        }

                        // 销售管理
                        try {
                            salePanel = new SalePanel(apiClient, mainFrameRef);
                            SwingUtilities.invokeLater(() -> {
                                tabbedPane.addTab("销售管理", salePanel);
                                publish("销售管理已加载");
                            });
                        } catch (Exception e) {
                            LogUtil.error("创建销售管理面板失败", e);
                            SwingUtilities.invokeLater(() -> {
                                updateStatus("销售管理面板加载失败: " + e.getMessage());
                            });
                        }

                        // 退货管理
                        try {
                            returnPanel = new ReturnPanel(apiClient, mainFrameRef);
                            SwingUtilities.invokeLater(() -> {
                                tabbedPane.addTab("退货管理", returnPanel);
                                publish("退货管理已加载");
                            });
                        } catch (Exception e) {
                            LogUtil.error("创建退货管理面板失败", e);
                            SwingUtilities.invokeLater(() -> {
                                updateStatus("退货管理面板加载失败: " + e.getMessage());
                            });
                        }

                        // 统计报表
                        try {
                            reportPanel = new ReportPanel(apiClient, mainFrameRef);
                            SwingUtilities.invokeLater(() -> {
                                tabbedPane.addTab("统计报表", reportPanel);
                                publish("统计报表已加载");
                            });
                        } catch (Exception e) {
                            LogUtil.error("创建统计报表面板失败", e);
                            SwingUtilities.invokeLater(() -> {
                                updateStatus("统计报表面板加载失败: " + e.getMessage());
                            });
                        }

                        // 回收站
                        try {
                            recycleBinPanel = new RecycleBinPanel(apiClient, mainFrameRef);
                            SwingUtilities.invokeLater(() -> {
                                tabbedPane.addTab("回收站", recycleBinPanel);
                                publish("回收站已加载");
                            });
                        } catch (Exception e) {
                            LogUtil.error("创建回收站面板失败", e);
                            SwingUtilities.invokeLater(() -> {
                                updateStatus("回收站面板加载失败: " + e.getMessage());
                            });
                        }

                        // 所有面板加载完成
                        SwingUtilities.invokeLater(() -> {
                            publish("所有页面加载完成");
                        });
                    } catch (Exception e) {
                        LogUtil.error("预加载选项卡时发生异常", e);
                        SwingUtilities.invokeLater(() -> {
                            updateStatus("加载页面时发生错误: " + e.getMessage());
                        });
                    }
                });

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    updateStatus(chunks.get(chunks.size() - 1));
                }
            }
        };

        worker.execute();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showUserInfo() {
        String message = String.format(
                "用户名: %s\n姓名: %s\n角色: %s\n登录时间: %s",
                userInfo.get("username"),
                userInfo.get("fullName"),
                userInfo.get("role"),
                java.time.LocalDateTime.now()
        );

        JOptionPane.showMessageDialog(this, message, "用户信息", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showPerformanceInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> requestStats = apiClient.getRequestStats();

            String message = String.format(
                    "内存使用:\n" +
                            "  可用: %,.2f MB\n" +
                            "  已用: %,.2f MB\n" +
                            "  最大: %,.2f MB\n\n" +
                            "请求统计:\n" +
                            "  总请求数: %s\n" +
                            "  失败请求数: %s\n" +
                            "  成功率: %s",
                    runtime.freeMemory() / 1024.0 / 1024.0,
                    (runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0,
                    runtime.maxMemory() / 1024.0 / 1024.0,
                    requestStats.get("totalRequests"),
                    requestStats.get("failedRequests"),
                    requestStats.get("successRate")
            );

            JOptionPane.showMessageDialog(this, message, "性能监控", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "获取性能信息失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showThreadPoolStatus() {
        String status = threadPoolManager.getPoolStatus();
        JOptionPane.showMessageDialog(this, status, "线程池状态", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        String message = "图书销售管理系统 v1.0\n" +
                "优化版本 - 性能增强版\n" +
                "开发团队: NorthGod\n" +
                "版权所有 © 2024\n\n" +
                "特性:\n" +
                "• HTTP连接池\n" +
                "• 异步加载\n" +
                "• 自动重试\n" +
                "• 自定义线程池\n" +
                "• 详细错误提示\n" +
                "• 实时数据获取";

        JOptionPane.showMessageDialog(this, message, "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshCache() {
        threadPoolManager.submitIoTask(() -> {
            updateStatus("正在刷新缓存...");
                // 客户端缓存已移除
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            updateStatus("缓存刷新完成");
        });
    }

    private void clearCache() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要清除所有缓存吗？",
                "确认",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
                // 客户端缓存已移除
            updateStatus("缓存已清除");
        }
    }

    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要登出吗？",
                "确认登出",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // 清除token
            apiClient.setToken(null);
            
            // 先关闭窗口（不清理线程池，因为可能还有其他操作）
            dispose();
            
            // 打开登录窗口
            SwingUtilities.invokeLater(() -> {
                try {
                    LoginFrame loginFrame = new LoginFrame();
                    loginFrame.setVisible(true);
                } catch (Exception e) {
                    LogUtil.error("创建登录窗口失败", e);
                    JOptionPane.showMessageDialog(null, 
                            "创建登录窗口失败: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void cleanup() {
        // 清理资源
        updateStatus("正在清理资源...");
        threadPoolManager.shutdown();
        updateStatus("资源清理完成");
    }
    
    /**
     * 通知所有面板刷新书籍下拉框
     * 当书籍被添加、编辑或删除时调用
     */
    public void refreshBookDropdowns() {
        SwingUtilities.invokeLater(() -> {
            if (purchasePanel != null) {
                purchasePanel.refreshBooks();
            }
            if (salePanel != null) {
                salePanel.refreshBooks();
            }
            LogUtil.info("已通知所有面板刷新书籍下拉框");
        });
    }
    
    /**
     * 刷新书籍管理页面的表格
     * 当进货、销售、退货等操作修改了书籍库存时调用
     */
    public void refreshBookPanel() {
        SwingUtilities.invokeLater(() -> {
            if (bookPanel != null) {
                bookPanel.loadBooksAsync();
                LogUtil.info("已刷新书籍管理页面");
            }
        });
    }
    
    /**
     * 刷新统计报表页面
     * 当进货、销售、退货等操作后调用
     */
    public void refreshReportPanel() {
        SwingUtilities.invokeLater(() -> {
            if (reportPanel != null) {
                reportPanel.refreshStatistics();
                LogUtil.info("已刷新统计报表页面");
            }
        });
    }
    
    /**
     * 刷新销售历史记录
     * 当销售操作后调用
     */
    public void refreshSaleHistory() {
        SwingUtilities.invokeLater(() -> {
            if (salePanel != null) {
                salePanel.refreshSaleHistory();
            }
            if (returnPanel != null) {
                returnPanel.refreshSaleHistory();
            }
            LogUtil.info("已刷新销售历史记录");
        });
    }
    
    /**
     * 通知所有面板刷新供应商下拉框
     * 当供应商被添加、编辑或删除时调用
     */
    public void refreshSupplierDropdowns() {
        SwingUtilities.invokeLater(() -> {
            if (purchasePanel != null) {
                purchasePanel.refreshSuppliers();
            }
            LogUtil.info("已通知所有面板刷新供应商下拉框");
        });
    }
    
    /**
     * 刷新供应商管理页面
     * 当供应商被恢复时调用
     */
    public void refreshSupplierPanel() {
        SwingUtilities.invokeLater(() -> {
            if (supplierPanel != null) {
                supplierPanel.loadSuppliersAsync();
                LogUtil.info("已刷新供应商管理页面");
            }
        });
    }
}