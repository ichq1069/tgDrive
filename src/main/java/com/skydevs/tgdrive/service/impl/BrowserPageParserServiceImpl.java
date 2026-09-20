package com.skydevs.tgdrive.service.impl;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import com.skydevs.tgdrive.config.PersistentBrowserManager;
import com.skydevs.tgdrive.service.WebPageParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("browserPageParser")
@Slf4j
public class BrowserPageParserServiceImpl implements WebPageParserService {

    private final WebPageParserService httpParser;
    private final PersistentBrowserManager browserManager;

    public BrowserPageParserServiceImpl(
            WebPageParserService webPageParserService,
            PersistentBrowserManager browserManager) {
        this.httpParser = webPageParserService;
        this.browserManager = browserManager;
    }

    @Override
    public ParseResult parseWebPage(String pageUrl, String cookie) {
        return parseWebPage(pageUrl, cookie, null, null, null);
    }

    @Override
    public ParseResult parseWebPage(String pageUrl, String cookie, String cssSelector) {
        return parseWebPage(pageUrl, cookie, null, null, cssSelector);
    }

    public ParseResult parseWebPage(String pageUrl, String cookie, Consumer<String> progressCallback) {
        return parseWebPage(pageUrl, cookie, null, progressCallback, null);
    }

    public ParseResult parseWebPage(String pageUrl, String cookie, String deviceMode, Consumer<String> progressCallback) {
        return parseWebPage(pageUrl, cookie, deviceMode, progressCallback, null);
    }

    public ParseResult parseWebPage(String pageUrl, String cookie, String deviceMode, Consumer<String> progressCallback, String cssSelector) {
        BrowserContext context = null;
        try {
            log.info("浏览器模式启动: URL={}, device={}", pageUrl, deviceMode);
            if (progressCallback != null) progressCallback.accept("正在启动浏览器...");

            // 根据设备模式创建不同的上下文
            boolean isH5 = "h5".equals(deviceMode);
            context = browserManager.createContextForDevice(isH5);

            // 注入Cookie
            if (cookie != null && !cookie.isEmpty()) {
                Page tempPage = context.newPage();
                String[] pairs = cookie.split(";");
                for (String pair : pairs) {
                    String trimmed = pair.trim();
                    if (!trimmed.isEmpty() && trimmed.contains("=")) {
                        try {
                            tempPage.evaluate("document.cookie = arguments[0];", trimmed);
                        } catch (Exception e) {
                            log.debug("Cookie注入失败: {}", trimmed);
                        }
                    }
                }
                tempPage.close();
            }

            Page page = context.newPage();

            if (progressCallback != null) progressCallback.accept("正在加载页面...");

            // 导航到目标页面
            page.navigate(pageUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // 智能等待：检测页面变化
            smartWait(page, progressCallback);

            String html = page.content();
            String title = page.title();

            if (progressCallback != null) progressCallback.accept("正在提取图片...");

            List<ImageInfo> images = extractImages(html, pageUrl);

            // 额外提取通过JS渲染的图片（支持CSS选择器限定范围）
            try {
                String imgSelector = (cssSelector != null && !cssSelector.trim().isEmpty())
                    ? cssSelector.trim() + " img"
                    : "img";
                @SuppressWarnings("unchecked")
                List<String> jsImages = (List<String>) page.evaluate(
                    "(sel) => Array.from(document.querySelectorAll(sel)).map(img => img.src || img.getAttribute('data-src')).filter(src => src && src.startsWith('http'))",
                    imgSelector
                );
                if (jsImages != null) {
                    for (String imgUrl : jsImages) {
                        if (images.stream().noneMatch(i -> i.getUrl().equals(imgUrl) && "image".equals(i.getType()))) {
                            images.add(new ImageInfo(imgUrl, ""));
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 额外提取通过JS渲染的视频（支持CSS选择器限定范围）
            try {
                String videoSelector = (cssSelector != null && !cssSelector.trim().isEmpty())
                    ? cssSelector.trim() + " video"
                    : "video";
                @SuppressWarnings("unchecked")
                List<String> jsVideos = (List<String>) page.evaluate(
                    "(sel) => Array.from(document.querySelectorAll(sel)).map(v => v.src || (v.querySelector('source') && v.querySelector('source').src)).filter(src => src && src.startsWith('http'))",
                    videoSelector
                );
                if (jsVideos != null) {
                    for (String videoUrl : jsVideos) {
                        if (images.stream().noneMatch(i -> i.getUrl().equals(videoUrl))) {
                            images.add(new ImageInfo(videoUrl, "", "video"));
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 如果指定了CSS选择器，也从选择器范围内的HTML提取
            if (cssSelector != null && !cssSelector.trim().isEmpty()) {
                try {
                    String scopedHtml = (String) page.evaluate(
                        "(sel) => { const el = document.querySelector(sel); return el ? el.innerHTML : ''; }",
                        cssSelector.trim()
                    );
                    if (scopedHtml != null && !scopedHtml.isEmpty()) {
                        List<ImageInfo> scopedImages = extractImages(scopedHtml, pageUrl);
                        for (ImageInfo img : scopedImages) {
                            if (images.stream().noneMatch(i -> i.getUrl().equals(img.getUrl()))) {
                                images.add(img);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            log.info("浏览器模式解析完成: URL={}, 标题={}, 图片数={}", pageUrl, title, images.size());
            if (progressCallback != null) progressCallback.accept("解析完成: 找到 " + images.size() + " 张图片");

            page.close();
            return new ParseResult(true, title, images, null);

        } catch (Exception e) {
            log.error("浏览器模式解析失败: {} -> {}", pageUrl, e.getMessage());
            // 浏览器模式失败时回退到HTTP模式
            log.info("回退到HTTP模式解析: {}", pageUrl);
            if (progressCallback != null) progressCallback.accept("浏览器模式失败，回退HTTP模式...");
            try {
                return httpParser.parseWebPage(pageUrl, cookie);
            } catch (Exception e2) {
                return new ParseResult(false, null, null, "浏览器和HTTP模式均解析失败: " + e.getMessage());
            }
        } finally {
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void smartWait(Page page, Consumer<String> progressCallback) throws InterruptedException {
        // 第一阶段：等待网络空闲（最多10秒）
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                new Page.WaitForLoadStateOptions().setTimeout(10000));
        } catch (Exception ignored) {}

        // 第二阶段：检测 Cloudflare challenge
        String html = page.content();
        if (html.contains("Just a moment") || html.contains("cf-challenge") || html.contains("Checking your browser")) {
            log.info("检测到 Cloudflare challenge，等待...");
            if (progressCallback != null) progressCallback.accept("检测到 Cloudflare challenge，等待验证...");

            // 等待 challenge 消失（最多40秒）
            long startTime = System.currentTimeMillis();
            long maxWait = 40000;
            while (System.currentTimeMillis() - startTime < maxWait) {
                Thread.sleep(2000);
                html = page.content();
                if (!html.contains("Just a moment") && !html.contains("cf-challenge") && !html.contains("Checking your browser")) {
                    log.info("Cloudflare challenge 已通过");
                    if (progressCallback != null) progressCallback.accept("Cloudflare 验证已通过");
                    break;
                }
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                if (progressCallback != null) progressCallback.accept("Cloudflare 验证中... (" + elapsed + "s)");
            }
        }

        // 第三阶段：等待页面稳定（检测图片加载）
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                new Page.WaitForLoadStateOptions().setTimeout(8000));
        } catch (Exception ignored) {}

        // 第四阶段：滚动触发懒加载
        try {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight / 3)");
            Thread.sleep(500);
            page.evaluate("window.scrollTo(0, document.body.scrollHeight * 2 / 3)");
            Thread.sleep(500);
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            Thread.sleep(1000);
        } catch (Exception ignored) {}
    }

    private List<ImageInfo> extractImages(String html, String baseUrl) {
        Set<String> seenUrls = new HashSet<>();
        List<ImageInfo> images = new ArrayList<>();

        // 提取 <img> 标签
        Pattern imgPattern = Pattern.compile(
            "<img[^>]+(?:data-src|data-original|data-lazy-src|src)\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher imgMatcher = imgPattern.matcher(html);
        while (imgMatcher.find()) {
            String imgUrl = resolveUrl(imgMatcher.group(1), baseUrl);
            if (imgUrl != null && !seenUrls.contains(imgUrl)) {
                seenUrls.add(imgUrl);
                String alt = extractAttr(imgMatcher.group(0), "alt");
                images.add(new ImageInfo(imgUrl, alt));
            }
        }

        // 提取 CSS background-image
        Pattern bgPattern = Pattern.compile(
            "background(?:-image)?\\s*:\\s*url\\s*\\(\\s*['\"]?([^'\"\\)]+)['\"]?\\s*\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher bgMatcher = bgPattern.matcher(html);
        while (bgMatcher.find()) {
            String imgUrl = resolveUrl(bgMatcher.group(1), baseUrl);
            if (imgUrl != null && !seenUrls.contains(imgUrl)) {
                seenUrls.add(imgUrl);
                images.add(new ImageInfo(imgUrl, ""));
            }
        }

        // 提取 srcset
        Pattern srcsetPattern = Pattern.compile(
            "srcset\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        Matcher srcsetMatcher = srcsetPattern.matcher(html);
        while (srcsetMatcher.find()) {
            String srcset = srcsetMatcher.group(1);
            for (String part : srcset.split(",")) {
                String[] tokens = part.trim().split("\\s+");
                if (tokens.length > 0) {
                    String imgUrl = resolveUrl(tokens[0], baseUrl);
                    if (imgUrl != null && !seenUrls.contains(imgUrl)) {
                        seenUrls.add(imgUrl);
                        images.add(new ImageInfo(imgUrl, ""));
                    }
                }
            }
        }

        // 提取 og:image
        Pattern ogPattern = Pattern.compile(
            "<meta[^>]+property\\s*=\\s*[\"']og:image[\"'][^>]+content\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        Matcher ogMatcher = ogPattern.matcher(html);
        while (ogMatcher.find()) {
            String imgUrl = resolveUrl(ogMatcher.group(1), baseUrl);
            if (imgUrl != null && !seenUrls.contains(imgUrl)) {
                seenUrls.add(imgUrl);
                images.add(new ImageInfo(imgUrl, ""));
            }
        }

        // 提取 <video> 标签
        Pattern videoPattern = Pattern.compile(
            "<video[^>]+(?:src|data-src)\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher videoMatcher = videoPattern.matcher(html);
        while (videoMatcher.find()) {
            String videoUrl = resolveUrl(videoMatcher.group(1), baseUrl);
            if (videoUrl != null && !seenUrls.contains(videoUrl)) {
                seenUrls.add(videoUrl);
                String poster = extractAttr(videoMatcher.group(0), "poster");
                images.add(new ImageInfo(videoUrl, poster, "video"));
            }
        }

        // 提取 <video> 内部 <source> 标签
        Pattern videoSourcePattern = Pattern.compile(
            "<source[^>]+src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*type\\s*=\\s*[\"']video/[^\"']+[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher videoSourceMatcher = videoSourcePattern.matcher(html);
        while (videoSourceMatcher.find()) {
            String videoUrl = resolveUrl(videoSourceMatcher.group(1), baseUrl);
            if (videoUrl != null && !seenUrls.contains(videoUrl)) {
                seenUrls.add(videoUrl);
                images.add(new ImageInfo(videoUrl, "", "video"));
            }
        }

        // 提取 <a> 标签中的视频直链
        Pattern aVideoPattern = Pattern.compile(
            "<a[^>]+href\\s*=\\s*[\"']([^\"']+(?:\\.mp4|\\.webm|\\.ogg|\\.mov)(?:\\?[^\"']*)?)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher aVideoMatcher = aVideoPattern.matcher(html);
        while (aVideoMatcher.find()) {
            String videoUrl = resolveUrl(aVideoMatcher.group(1), baseUrl);
            if (videoUrl != null && !seenUrls.contains(videoUrl)) {
                seenUrls.add(videoUrl);
                images.add(new ImageInfo(videoUrl, "", "video"));
            }
        }

        return images;
    }

    private String extractAttr(String tag, String attrName) {
        Pattern pattern = Pattern.compile(attrName + "\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(tag);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String resolveUrl(String url, String baseUrl) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        if (url.startsWith("data:") || url.startsWith("javascript:") || url.startsWith("#")) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL resolved = new java.net.URL(base, url);
            return resolved.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
