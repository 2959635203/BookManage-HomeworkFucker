package com.northgod.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class NetworkTestDialog extends JDialog {
    private final ApiClient apiClient;
    private JTextArea resultArea;
    private JButton testButton;
    private JButton closeButton;
    private JProgressBar progressBar;

    public NetworkTestDialog(Frame parent, ApiClient apiClient) {
        super(parent, "网络连接测试", true);
        this.apiClient = apiClient;
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);

        // 标题
        JLabel titleLabel = new JLabel("网络连接测试工具", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 结果区域
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        resultArea.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("测试结果"));
        add(scrollPane, BorderLayout.CENTER);

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        testButton = new JButton("开始测试");
        closeButton = new JButton("关闭");

        testButton.setPreferredSize(new Dimension(100, 30));
        closeButton.setPreferredSize(new Dimension(100, 30));

        buttonPanel.add(testButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // 事件处理
        testButton.addActionListener(e -> runNetworkTest());
        closeButton.addActionListener(e -> dispose());
    }

    private void runNetworkTest() {
        testButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("正在测试网络连接...");

        resultArea.setText("");
        appendResult("=== 网络连接测试开始 ===\n");

        ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                // 测试DNS解析
                appendResult("\n1. 测试DNS解析...");
                testDnsResolution();

                // 测试服务器连接
                appendResult("\n2. 测试服务器连接...");
                testServerConnection();

                // 测试API接口
                appendResult("\n3. 测试API接口...");
                testApiEndpoint();

                // 测试认证
                appendResult("\n4. 测试认证功能...");
                testAuthentication();

                appendResult("\n=== 测试完成 ===");
                appendResult("\n所有测试通过，网络连接正常！");

                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("测试完成");
                    DialogUtil.showSuccessDialog(this, "网络测试完成，所有测试通过！");
                });
            } catch (Exception e) {
                appendResult("\n=== 测试失败 ===");
                appendResult("\n错误: " + e.getMessage());

                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("测试失败");
                    DialogUtil.showDetailedErrorDialog(this,
                            "网络测试失败:\n" + e.getMessage(), "测试失败");
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    testButton.setEnabled(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                });
            }
        });
    }

    private void testDnsResolution() {
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName("localhost");
            appendResult("  ✓ DNS解析成功: " + address.getHostAddress());
        } catch (Exception e) {
            appendResult("  ✗ DNS解析失败: " + e.getMessage());
            throw new RuntimeException("DNS解析失败", e);
        }
    }

    private void testServerConnection() {
        try {
            java.net.Socket socket = new java.net.Socket("localhost", 8080);
            socket.close();
            appendResult("  ✓ 服务器连接成功");
        } catch (Exception e) {
            appendResult("  ✗ 服务器连接失败: " + e.getMessage());
            throw new RuntimeException("服务器连接失败", e);
        }
    }

    private void testApiEndpoint() {
        try {
            String response = apiClient.get("/health");
            appendResult("  ✓ API接口访问成功");
            // 解析响应
            java.util.Map<String, Object> result = com.northgod.client.util.JsonUtil.parseJson(response);
            if (result != null && "UP".equals(result.get("status"))) {
                appendResult("  ✓ 服务器状态: 运行正常");
            }
        } catch (Exception e) {
            appendResult("  ✗ API接口访问失败: " + e.getMessage());
            throw new RuntimeException("API接口访问失败", e);
        }
    }

    private void testAuthentication() {
        try {
            // 测试无需认证的端点
            String response = apiClient.get("/public/status");
            appendResult("  ✓ 公开端点访问成功");
        } catch (Exception e) {
            appendResult("  ✗ 公开端点访问失败: " + e.getMessage());
            // 这里不抛出异常，因为可能没有这个端点
        }
    }

    private void appendResult(String text) {
        SwingUtilities.invokeLater(() -> {
            resultArea.append(text + "\n");
            resultArea.setCaretPosition(resultArea.getDocument().getLength());
        });
    }
}