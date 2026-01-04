package com.northgod.client.util;

import com.northgod.client.ui.panel.BookPanel;

import javax.swing.*;
import java.awt.*;

public class DialogUtil {

    public static void showSuccessDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showErrorDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public static void showWarningDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "警告", JOptionPane.WARNING_MESSAGE);
    }

    public static void showInfoDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean showConfirmDialog(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, "确认",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    @SuppressWarnings("unused")
    public static String showInputDialog(Component parent, String message, String title) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * 显示详细的错误对话框，包含更多信息和解决方案
     */
    public static void showDetailedErrorDialog(Component parent, String message, String title) {
        // 创建自定义对话框
        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                title,
                true
        );

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setMinimumSize(new Dimension(500, 350));

        // 错误图标
        JPanel iconPanel = new JPanel();
        Icon errorIcon = UIManager.getIcon("OptionPane.errorIcon");
        iconPanel.add(new JLabel(errorIcon));

        // 错误信息面板
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(UIManager.getColor("Panel.background"));
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("错误详情"));
        scrollPane.setPreferredSize(new Dimension(480, 200));

        // 解决方案面板
        JTextArea solutionArea = new JTextArea("""
                建议解决方案:
                1. 检查网络连接是否正常
                2. 确认服务器是否正在运行
                3. 检查防火墙设置
                4. 稍后重试操作
                5. 联系技术支持获取帮助""");
        solutionArea.setEditable(false);
        solutionArea.setLineWrap(true);
        solutionArea.setWrapStyleWord(true);
        solutionArea.setBackground(UIManager.getColor("Panel.background"));
        solutionArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        solutionArea.setForeground(new Color(0, 100, 0));

        JScrollPane solutionScroll = new JScrollPane(solutionArea);
        solutionScroll.setBorder(BorderFactory.createTitledBorder("解决方案"));
        solutionScroll.setPreferredSize(new Dimension(480, 100));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("确定");
        JButton copyButton = new JButton("复制错误信息");
        JButton retryButton = new JButton("重试");

        buttonPanel.add(copyButton);
        buttonPanel.add(retryButton);
        buttonPanel.add(okButton);

        // 添加组件
        dialog.add(iconPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.add(scrollPane, BorderLayout.NORTH);
        contentPanel.add(solutionScroll, BorderLayout.CENTER);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 事件处理
        okButton.addActionListener(e -> dialog.dispose());

        copyButton.addActionListener(e -> {
            textArea.selectAll();
            textArea.copy();
            JOptionPane.showMessageDialog(dialog, "错误信息已复制到剪贴板", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        retryButton.addActionListener(e -> {
            dialog.dispose();
            // 检查父组件是否是BookPanel，然后调用公共的loadBooksAsync方法
            if (parent instanceof BookPanel) {
                ((BookPanel) parent).loadBooksAsync();
            } else {
                // 如果不是BookPanel，可能是其他面板需要重试操作
                showInfoDialog(parent, "重试功能在当前上下文中不可用", "提示");
            }
        });

        // 显示对话框
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}