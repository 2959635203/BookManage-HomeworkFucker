package com.northgod.client.ui;

import com.northgod.client.service.ApiClient;
import com.northgod.client.util.DialogUtil;
import com.northgod.client.util.LogUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private ApiClient apiClient;
    private JButton loginButton;
    private JButton cancelButton;
    private JProgressBar progressBar;

    public LoginFrame() {
        apiClient = new ApiClient();
        initComponents();
    }

    private void initComponents() {
        setTitle("图书销售管理系统 - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 350);
        setLocationRelativeTo(null);
        setResizable(false);

        // 使用BorderLayout
        setLayout(new BorderLayout(10, 10));

        // 标题面板
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("图书销售管理系统");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        usernameField.setToolTipText("请输入用户名");
        formPanel.add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("密码:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        passwordField.setToolTipText("请输入密码");
        formPanel.add(passwordField, gbc);

        // 按钮面板
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        loginButton = new JButton("登录");
        cancelButton = new JButton("取消");

        loginButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setPreferredSize(new Dimension(100, 30));

        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        formPanel.add(buttonPanel, gbc);

        add(formPanel, BorderLayout.CENTER);

        // 进度条（初始隐藏）
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.SOUTH);

        // 底部信息面板
        JPanel infoPanel = new JPanel();
        infoPanel.add(new JLabel("默认账号: admin 密码: admin123"));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(infoPanel, BorderLayout.SOUTH);

        // 事件处理
        loginButton.addActionListener(e -> login());
        cancelButton.addActionListener(e -> System.exit(0));

        // 设置默认按钮
        getRootPane().setDefaultButton(loginButton);

        // 设置用户名焦点
        SwingUtilities.invokeLater(() -> usernameField.requestFocus());
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            DialogUtil.showErrorDialog(this, "用户名和密码不能为空");
            return;
        }

        // 禁用登录按钮，显示进度条
        loginButton.setEnabled(false);
        cancelButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("正在登录...");

        // 在新线程中执行登录
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                try {
                    return apiClient.login(username, password);
                } catch (Exception e) {
                    LogUtil.error("登录过程中发生异常: " + username, e);
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "网络连接失败，请检查服务器是否启动";
                    }
                    return Map.of("success", false, "message", errorMessage);
                }
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> response = get();

                    if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                        Object dataObj = response.get("data");
                        if (dataObj == null) {
                            throw new Exception("服务器返回数据格式错误：data字段为空");
                        }
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        
                        String token = (String) data.get("token");
                        if (token == null || token.isEmpty()) {
                            throw new Exception("服务器返回数据格式错误：token为空");
                        }
                        
                        apiClient.setToken(token);

                        // 创建用户对象，安全处理可能为null的字段
                        Map<String, Object> userInfo = new java.util.HashMap<>();
                        userInfo.put("username", data.get("username") != null ? data.get("username") : username);
                        userInfo.put("fullName", data.get("fullName") != null ? data.get("fullName") : "");
                        userInfo.put("role", data.get("role") != null ? data.get("role") : "STAFF");
                        userInfo.put("token", token);

                        // 登录成功，打开主界面
                        SwingUtilities.invokeLater(() -> {
                            MainFrame mainFrame = new MainFrame(apiClient, userInfo);
                            mainFrame.setVisible(true);
                            dispose();
                            LogUtil.info("用户 " + username + " 登录成功，打开主界面");
                        });

                        progressBar.setString("登录成功，正在跳转...");
                    } else {
                        String message = "登录失败";
                        if (response != null) {
                            Object msgObj = response.get("message");
                            if (msgObj != null) {
                                message = msgObj.toString();
                            }
                        }

                        if (message.contains("Connection refused") || message.contains("连接被拒绝")) {
                            String detailedMessage = "无法连接到服务器\n\n" +
                                    "可能原因:\n" +
                                    "1. 服务器未启动\n" +
                                    "2. 服务器地址配置错误\n" +
                                    "3. 网络连接异常\n\n" +
                                    "解决方案:\n" +
                                    "1. 确认服务器已启动\n" +
                                    "2. 检查配置文件中的服务器地址\n" +
                                    "3. 检查网络连接";

                            DialogUtil.showDetailedErrorDialog(LoginFrame.this, detailedMessage, "连接失败");
                        } else if (message.contains("401") || message.contains("认证失败")) {
                            DialogUtil.showErrorDialog(LoginFrame.this, "用户名或密码错误，请重新输入");
                            passwordField.setText("");
                            passwordField.requestFocus();
                        } else {
                            DialogUtil.showDetailedErrorDialog(LoginFrame.this,
                                    "登录失败: " + message, "登录错误");
                        }

                        LogUtil.warn("用户登录失败: " + username + " - " + message);
                        progressBar.setString("登录失败");
                    }
                } catch (Exception ex) {
                    LogUtil.error("登录结果处理异常", ex);
                    
                    // 提取详细的错误信息
                    String errorMsg = ex.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = ex.getClass().getSimpleName();
                    }
                    
                    // 检查是否是网络连接错误
                    String detailedMessage;
                    if (errorMsg.contains("Connection refused") || 
                        errorMsg.contains("连接被拒绝") ||
                        errorMsg.contains("无法连接到服务器")) {
                        detailedMessage = "无法连接到服务器\n\n" +
                                "可能原因:\n" +
                                "1. 服务器未启动\n" +
                                "2. 服务器地址配置错误\n" +
                                "3. 网络连接异常\n\n" +
                                "解决方案:\n" +
                                "1. 确认服务器已启动（检查Server项目是否运行）\n" +
                                "2. 检查配置文件中的服务器地址（application.properties）\n" +
                                "3. 检查网络连接\n\n" +
                                "详细错误: " + errorMsg;
                    } else if (errorMsg.contains("401") || 
                               errorMsg.contains("用户名或密码错误") ||
                               errorMsg.contains("LOGIN_FAILED")) {
                        detailedMessage = "用户名或密码错误\n\n请检查:\n" +
                                "1. 用户名是否正确\n" +
                                "2. 密码是否正确\n" +
                                "3. 默认账号: admin / admin123";
                        DialogUtil.showErrorDialog(LoginFrame.this, detailedMessage);
                        passwordField.setText("");
                        passwordField.requestFocus();
                    } else {
                        detailedMessage = "登录过程中发生错误\n\n" +
                                "错误信息: " + errorMsg + "\n\n" +
                                "建议操作:\n" +
                                "1. 检查服务器是否正常运行\n" +
                                "2. 检查网络连接\n" +
                                "3. 查看日志文件获取详细信息";
                        DialogUtil.showDetailedErrorDialog(LoginFrame.this, detailedMessage, "系统错误");
                    }
                    
                    progressBar.setString("发生错误");
                } finally {
                    // 恢复界面状态
                    loginButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                }
            }
        };

        worker.execute();
    }
}