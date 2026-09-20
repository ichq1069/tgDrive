package com.skydevs.tgdrive.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class PersistentBrowserManager {

    private Playwright playwright;
    private Browser browser;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile long lastUsedAt = System.currentTimeMillis();
    private volatile boolean restarting = false;

    private static final long IDLE_TIMEOUT_MS = 30 * 60 * 1000; // 30分钟空闲后关闭

    public PersistentBrowserManager() {
        initBrowser();
        // 启动空闲检查线程
        Thread idleChecker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60_000); // 每分钟检查
                    if (browser != null && browser.isConnected()
                        && System.currentTimeMillis() - lastUsedAt > IDLE_TIMEOUT_MS) {
                        log.info("浏览器空闲超时，关闭以释放资源");
                        closeBrowser();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "browser-idle-checker");
        idleChecker.setDaemon(true);
        idleChecker.start();
    }

    private void initBrowser() {
        lock.lock();
        try {
            if (browser != null && browser.isConnected()) {
                return;
            }
            closeBrowserInternal();

            log.info("初始化持久化浏览器实例...");
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.List.of(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-web-security",
                        "--disable-features=IsolateOrigins,site-per-process",
                        "--disable-blink-features=AutomationControlled"
                    ))
            );
            lastUsedAt = System.currentTimeMillis();
            log.info("持久化浏览器实例初始化完成");
        } catch (Exception e) {
            log.error("浏览器初始化失败", e);
            throw new RuntimeException("浏览器初始化失败", e);
        } finally {
            lock.unlock();
        }
    }

    public BrowserContext createContext() {
        return createContextForDevice(false);
    }

    public BrowserContext createContextForDevice(boolean isH5) {
        ensureBrowserAlive();
        lastUsedAt = System.currentTimeMillis();

        if (isH5) {
            // H5/移动端模式
            return browser.newContext(
                new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
                    .setViewportSize(390, 844)
                    .setLocale("zh-CN")
                    .setIsMobile(true)
                    .setHasTouch(true)
            );
        } else {
            // PC/桌面模式
            return browser.newContext(
                new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale("zh-CN")
            );
        }
    }

    private void ensureBrowserAlive() {
        if (browser != null && browser.isConnected()) {
            return;
        }
        log.warn("浏览器已断开，重新初始化...");
        if (!restarting) {
            restarting = true;
            try {
                initBrowser();
            } finally {
                restarting = false;
            }
        } else {
            // 等待其他线程完成重启
            int waitCount = 0;
            while (restarting && waitCount < 30) {
                try {
                    Thread.sleep(1000);
                    waitCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (browser == null || !browser.isConnected()) {
                throw new RuntimeException("浏览器重启超时");
            }
        }
    }

    private void closeBrowser() {
        lock.lock();
        try {
            closeBrowserInternal();
        } finally {
            lock.unlock();
        }
    }

    private void closeBrowserInternal() {
        try {
            if (browser != null) {
                browser.close();
                browser = null;
            }
        } catch (Exception ignored) {}
        try {
            if (playwright != null) {
                playwright.close();
                playwright = null;
            }
        } catch (Exception ignored) {}
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭持久化浏览器实例...");
        closeBrowser();
    }
}
