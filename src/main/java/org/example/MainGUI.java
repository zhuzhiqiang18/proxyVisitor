package org.example;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.*;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import com.example.ProxyVisitor;

public class MainGUI extends JFrame {
    private JTextArea logArea;
    private final JButton startButton;
    private final JButton stopButton;
    private final JSpinner minIntervalSpinner;
    private final JSpinner maxIntervalSpinner;
    private final JComboBox<com.example.ProxyVisitor.VisitMode> visitModeCombo;
    private final JComboBox<com.example.ProxyVisitor.VisitViewMode> visitViewModeCombo;

    private final AtomicReference<com.example.ProxyVisitor> visitorRef;
    private final JLabel statusLabel;

    public MainGUI() {
        setTitle("代理访问工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        visitorRef = new AtomicReference<>();

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setUIFont(new FontUIResource("Microsoft YaHei", Font.PLAIN, 12));



        // 创建控制面板
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 访问模式选择
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("访问模式:"), gbc);

        gbc.gridx = 1;
        visitModeCombo = new JComboBox<>(com.example.ProxyVisitor.VisitMode.values());
        controlPanel.add(visitModeCombo, gbc);

        gbc.gridx = 2;
        visitViewModeCombo = new JComboBox<>(com.example.ProxyVisitor.VisitViewMode.values());
        controlPanel.add(visitViewModeCombo, gbc);

        // 间隔设置
        gbc.gridx = 0;
        gbc.gridy = 1;
        controlPanel.add(new JLabel("访问间隔(秒):"), gbc);

        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        minIntervalSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        maxIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 3600, 1));
        intervalPanel.add(minIntervalSpinner);
        intervalPanel.add(new JLabel("到"));
        intervalPanel.add(maxIntervalSpinner);

        gbc.gridx = 1;
        controlPanel.add(intervalPanel, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        startButton = new JButton("开始访问");
        stopButton = new JButton("停止访问");
        JButton configButton = new JButton("配置文件");
        stopButton.setEnabled(false);

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(configButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        controlPanel.add(buttonPanel, gbc);

        // 状态标签
        statusLabel = new JLabel("就绪");
        gbc.gridy = 3;
        controlPanel.add(statusLabel, gbc);

        // 日志区域
        logArea = new JTextArea();
        logArea.setEditable(false);

        setupLogArea();


        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        // 添加组件到主面板
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 添加事件监听器
        startButton.addActionListener(e -> startVisitor());
        stopButton.addActionListener(e -> stopVisitor());
        configButton.addActionListener(e -> openConfigFiles());

        // 设置窗口内容
        setContentPane(mainPanel);

        // 重定向System.out到日志区域
        redirectSystemOut();
    }


    // 添加设置全局字体的方法
    private void setUIFont(FontUIResource f) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }


    private void setupLogArea() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        // 设置字体
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        // 设置行距
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    }

    private void redirectSystemOut() {
        try {
            // 创建带缓冲的输出流
            PipedInputStream pin = new PipedInputStream();
            PipedOutputStream pout = new PipedOutputStream(pin);

            // 创建新的PrintStream，指定UTF-8编码
            System.setOut(new PrintStream(pout, true, "UTF-8"));

            // 创建读取线程
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(pin, "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String finalLine = line;
                        SwingUtilities.invokeLater(() -> {
                            logArea.append(finalLine + "\n");
                            // 自动滚动到底部
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVisitor() {
        try {
            int minInterval = (int) minIntervalSpinner.getValue();
            int maxInterval = (int) maxIntervalSpinner.getValue();

            if (minInterval > maxInterval) {
                JOptionPane.showMessageDialog(this, "最小间隔不能大于最大间隔！");
                return;
            }

            com.example.ProxyVisitor visitor = new com.example.ProxyVisitor(
                    minInterval,
                    maxInterval,
                    (com.example.ProxyVisitor.VisitMode) visitModeCombo.getSelectedItem(),
                    (ProxyVisitor.VisitViewMode) visitViewModeCombo.getSelectedItem()
            );

            visitor.loadUrlsFromFile("urls.txt");
            visitor.loadProxiesFromFile("proxies.txt");

            if (visitor.getUrlCount() == 0 || visitor.getProxyCount() == 0) {
                throw new IllegalStateException("URL或代理配置为空");
            }

            visitor.startVisiting();
            visitorRef.set(visitor);

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("正在运行中...");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopVisitor() {
        ProxyVisitor visitor = visitorRef.get();
        if (visitor != null && visitor.isRunning()) {
            visitor.stopVisiting();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("已停止");
        }
    }

    private void openConfigFiles() {
        try {
            Desktop desktop = Desktop.getDesktop();
            File urlsFile = new File("urls.txt");
            File proxiesFile = new File("proxies.txt");

            if (!urlsFile.exists()) {
                createDefaultUrlsFile(urlsFile);
            }
            if (!proxiesFile.exists()) {
                createDefaultProxiesFile(proxiesFile);
            }

            desktop.edit(urlsFile);
            desktop.edit(proxiesFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "打开配置文件失败: " + e.getMessage());
        }
    }

    private void createDefaultUrlsFile(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("# 每行一个URL");
            writer.println("# 示例:");
            writer.println("# https://example.com");
            writer.println("# https://example.org");
        }
    }

    private void createDefaultProxiesFile(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("# 格式: 代理地址 端口 用户名 密码");
            writer.println("# 示例:");
            writer.println("# proxy.example.com 1080 username password");
        }
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> {
            new MainGUI().setVisible(true);
        });
    }
}