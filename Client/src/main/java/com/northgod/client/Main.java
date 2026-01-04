package com.northgod.client;

import java.awt.Font;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.northgod.client.ui.LoginFrame;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class Main {
    public static void main(String[] args) {
        // 设置未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LogUtil.error("未捕获的异常: " + thread.getName(), throwable);

            SwingUtilities.invokeLater(() -> {
                String errorMessage = "应用程序发生未捕获异常:\n\n" +
                        "线程: " + thread.getName() + "\n" +
                        "错误: " + throwable.getMessage() + "\n\n" +
                        "建议操作:\n" +
                        "1. 重启应用程序\n" +
                        "2. 联系技术支持\n" +
                        "3. 查看日志文件获取详细信息";

                JOptionPane.showMessageDialog(null, errorMessage,
                        "系统错误", JOptionPane.ERROR_MESSAGE);
            });
        });

        // 设置Swing未捕获异常处理器
        SwingUtilities.invokeLater(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                LogUtil.error("Swing线程未捕获异常: " + thread.getName(), throwable);

                String errorMessage = "界面线程发生异常:\n\n" +
                        "错误: " + throwable.getMessage() + "\n\n" +
                        "应用程序可能需要重启";

                JOptionPane.showMessageDialog(null, errorMessage,
                        "界面错误", JOptionPane.ERROR_MESSAGE);
            });
        });

        // 设置系统编码为UTF-8
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        System.setProperty("user.language", "zh");
        System.setProperty("user.region", "CN");
        System.setProperty("user.country", "CN");

        // 设置标准输入输出流的编码
        try {
            // 使用反射设置控制台编码（Windows PowerShell支持）
            try {
                java.lang.reflect.Field charsetField = PrintStream.class.getDeclaredField("charset");
                charsetField.setAccessible(true);
            } catch (Exception ignored) {
                // 忽略反射失败
            }
            
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
            
            // 尝试设置控制台编码（Windows）
            try {
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    // Windows下设置控制台代码页为UTF-8
                    new ProcessBuilder("cmd", "/c", "chcp", "65001").start();
                }
            } catch (Exception ignored) {
                // 忽略设置代码页失败
            }
        } catch (Exception e) {
            // 如果设置失败，至少记录错误但不影响程序运行
            System.err.println("警告: 设置UTF-8编码失败，可能影响中文显示: " + e.getMessage());
        }

        // 设置Swing外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // 设置UI字体
            setUIFont();

            // 设置一些UI属性
            UIManager.put("Button.font", new Font("微软雅黑", Font.PLAIN, 12));
            UIManager.put("Label.font", new Font("微软雅黑", Font.PLAIN, 12));
            UIManager.put("TextField.font", new Font("微软雅黑", Font.PLAIN, 12));
            UIManager.put("TextArea.font", new Font("微软雅黑", Font.PLAIN, 12));
            UIManager.put("Table.font", new Font("微软雅黑", Font.PLAIN, 12));
            UIManager.put("TableHeader.font", new Font("微软雅黑", Font.BOLD, 12));
            UIManager.put("TitledBorder.font", new Font("微软雅黑", Font.BOLD, 12));

        } catch (Exception e) {
            LogUtil.error("设置Swing外观失败", e);
        }

        // 启动登录界面
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);

                LogUtil.info("应用程序启动成功");
            } catch (Exception e) {
                LogUtil.error("启动应用程序失败", e);

                String errorMessage = "启动应用程序失败:\n\n" +
                        "错误: " + e.getMessage() + "\n\n" +
                        "可能原因:\n" +
                        "1. Java环境不兼容\n" +
                        "2. 缺少必要的库文件\n" +
                        "3. 系统资源不足\n\n" +
                        "请检查系统要求并重试";

                JOptionPane.showMessageDialog(null, errorMessage,
                        "启动失败", JOptionPane.ERROR_MESSAGE);

                // 确保线程池关闭
                ThreadPoolManager.getInstance().shutdown();
                System.exit(1);
            }
        });

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LogUtil.info("应用程序正在关闭...");
            ThreadPoolManager.getInstance().shutdown();
            com.northgod.client.network.HttpConnectionManager.shutdown();
            LogUtil.info("应用程序已关闭");
            LogUtil.close(); // 关闭日志文件流
        }));
    }

    private static void setUIFont() {
        // 设置UI字体
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource("微软雅黑", Font.PLAIN, 12));
            }
        }
    }
}