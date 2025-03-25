package com.example;

import org.example.PlaywrightUtil;
import org.example.ProxyConfig;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ProxyVisitor {
    private List<ProxyConfig> proxyPool;
    private List<String> targetUrls;
    private int minInterval;
    private int maxInterval;
    private Random random;
    private ScheduledExecutorService scheduler;
    private boolean isRandomMode;
    private VisitViewMode visitViewMode;
    private int currentUrlIndex;
    private volatile boolean isRunning;
    private ScheduledFuture<?> scheduledFuture;

    public enum VisitMode {
        SEQUENTIAL("顺序访问"),
        RANDOM("随机访问");

        private final String description;

        VisitMode(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public enum VisitViewMode {
        PT("普通访问"),
        CHROME("浏览器访问");

        private final String description;

        VisitViewMode(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public ProxyVisitor(int minInterval, int maxInterval, VisitMode mode, VisitViewMode viewMode) {
        this.proxyPool = new ArrayList<>();
        this.targetUrls = new ArrayList<>();
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.random = new Random();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRandomMode = (mode == VisitMode.RANDOM);
        this.currentUrlIndex = 0;
        this.isRunning = false;
        this.visitViewMode = viewMode;

        setupAuthenticator();
    }

    private void setupAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                for (ProxyConfig config : proxyPool) {
                    if (getRequestingHost().equals(config.getHost())
                            && getRequestingPort() == config.getPort()) {
                        return new PasswordAuthentication(
                                config.getUsername(),
                                config.getPassword().toCharArray()
                        );
                    }
                }
                return null;
            }
        });
    }

    public void loadUrlsFromFile(String filePath) throws IOException {
        targetUrls.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    try {
                        new URL(line);
                        targetUrls.add(line);
                    } catch (MalformedURLException e) {
                        System.out.println("无效URL: " + line);
                    }
                }
            }
        }
    }

    public void loadProxiesFromFile(String filePath) throws IOException {
        proxyPool.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 4) {
                        try {
                            String host = parts[0];
                            int port = Integer.parseInt(parts[1]);
                            addProxy(host, port, parts[2], parts[3]);
                        } catch (NumberFormatException e) {
                            System.out.println("无效代理配置: " + line);
                        }
                    }
                }
            }
        }
    }

    public void addProxy(String host, int port, String username, String password) {
        proxyPool.add(new ProxyConfig(host, port, username, password));
    }

    private String getNextUrl() {
        if (targetUrls.isEmpty()) return null;
        if (isRandomMode) {
            return targetUrls.get(random.nextInt(targetUrls.size()));
        } else {
            String url = targetUrls.get(currentUrlIndex);
            currentUrlIndex = (currentUrlIndex + 1) % targetUrls.size();
            return url;
        }
    }

    private void scheduleNextVisit() {
        if (!isRunning) return;

        int delay = minInterval + random.nextInt(maxInterval - minInterval + 1);
        scheduledFuture = scheduler.schedule(() -> {
            visitWebsite();
            scheduleNextVisit();
        }, delay, TimeUnit.SECONDS);
    }

    private void visitWebsite() {
        if (!isRunning) return;

        ProxyConfig proxyConfig = proxyPool.get(random.nextInt(proxyPool.size()));
        String targetUrl = getNextUrl();

        if (proxyConfig == null || targetUrl == null) {
            return;
        }

        System.setProperty("socksProxyHost", proxyConfig.getHost());
        System.setProperty("socksProxyPort", String.valueOf(proxyConfig.getPort()));
        System.setProperty("java.net.socks.username", proxyConfig.getUsername());
        System.setProperty("java.net.socks.password", proxyConfig.getPassword());

        if (visitViewMode == VisitViewMode.PT){
            ptVisit(proxyConfig, targetUrl);
        }
        else if (visitViewMode == VisitViewMode.CHROME){
            chromeVisit(proxyConfig, targetUrl);
        }


    }


    public void ptVisit(ProxyConfig proxyConfig, String targetUrl) {
        try {


            Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()));

            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/91.0.4472.124");

            int responseCode = conn.getResponseCode();
            System.out.println(String.format(
                    "访问完成 - URL: %s - 代理: %s - 响应码: %d",
                    targetUrl, proxyConfig, responseCode
            ));

            conn.disconnect();
        } catch (Exception e) {
            System.out.println(String.format(
                    "访问失败 - URL: %s - 代理: %s - 错误: %s",
                    targetUrl, proxyConfig, e.getMessage()
            ));
        }
    }

    public void chromeVisit(ProxyConfig proxyConfig, String targetUrl){
        com.microsoft.playwright.options.Proxy proxy = new com.microsoft.playwright.options.Proxy("socks5://"+proxyConfig.getHost())
                //.setBypass(proxyConfig.getHost())
                .setUsername(proxyConfig.getUsername())
                .setPassword(proxyConfig.getPassword());

        try {

            PlaywrightUtil.getHtml(targetUrl,proxy);
            System.out.println(String.format(
                    "访问完成 - URL: %s - 代理: %s - 响应码: %d",
                    targetUrl, proxyConfig, 200
            ));
        } catch (Exception e) {
            System.out.println(String.format(
                    "访问失败 - URL: %s - 代理: %s - 错误: %s",
                    targetUrl, proxyConfig, e.getMessage()
            ));
        }

    }

    public void startVisiting() {
        if (isRunning) return;
        if (proxyPool.isEmpty() || targetUrls.isEmpty()) {
            throw new IllegalStateException("代理池或URL列表为空");
        }

        isRunning = true;
        scheduleNextVisit();
    }

    public void stopVisiting() {
        if (!isRunning) return;
        isRunning = false;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduler.shutdown();
    }

    public void setVisitMode(VisitMode mode) {
        this.isRandomMode = (mode == VisitMode.RANDOM);
        this.currentUrlIndex = 0;
    }

    public void setIntervals(int minInterval, int maxInterval) {
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getUrlCount() {
        return targetUrls.size();
    }

    public int getProxyCount() {
        return proxyPool.size();
    }
}