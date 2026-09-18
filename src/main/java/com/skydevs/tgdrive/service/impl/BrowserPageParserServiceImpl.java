package com.skydevs.tgdrive.service.impl;

import com.microsoft.playwright.*;
import com.skydevs.tgdrive.service.WebPageParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BrowserPageParserServiceImpl implements WebPageParserService {

    private static Playwright playwright;

    private static synchronized Playwright getPlaywright() {
        if (playwright == null) {
            playwright = Playwright.create();
        }
        return playwright;
    }

    @Override
    public ParseResult parseWebPage(String pageUrl, String cookie) {
        Browser browser = null;
        try {
            Playwright pw = getPlaywright();
            browser = pw.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu"
                    ))
            );

            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale("zh-CN")
            );

            // 注入Cookie
            if (cookie != null && !cookie.isEmpty()) {
                List<Cookie> cookies = parseCookieString(cookie, pageUrl);
                if (!cookies.isEmpty()) {
                    context.addCookies(cookies);
                }
            }

            Page page = context.newPage();
            page.navigate(pageUrl, new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));

            // 等待页面加载
            page.waitForTimeout(3000);

            // 尝试等待图片加载
            try {
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE, 
                    new Page.WaitForLoadStateOptions().setTimeout(10000));
            } catch (Exception ignored) {}

            String html = page.content();
            String title = page.title();

            List<ImageInfo> images = extractImages(html, pageUrl);

            // 额外提取通过JS渲染的图片
            try {
                List<String> jsImages = page.evaluate(
                    "() => Array.from(document.querySelectorAll('img')).map(img => img.src).filter(src => src && src.startsWith('http'))"
                );
                for (String imgUrl : jsImages) {
                    if (!images.stream().anyMatch(i -> i.getUrl().equals(imgUrl))) {
                        images.add(new ImageInfo(imgUrl, ""));
                    }
                }
            } catch (Exception ignored) {}

            log.info("浏览器模式解析完成: URL={}, 标题={}, 图片数={}", pageUrl, title, images.size());
            return new ParseResult(true, title, images, null);

        } catch (Exception e) {
            log.error("浏览器模式解析失败: {} -> {}", pageUrl, e.getMessage());
            return new ParseResult(false, null, null, "浏览器解析失败: " + e.getMessage());
        } finally {
            if (browser != null) {
                try { browser.close(); } catch (Exception ignored) {}
            }
        }
    }

    private List<Cookie> parseCookieString(String cookieStr, String pageUrl) {
        List<Cookie> cookies = new ArrayList<>();
        try {
            String domain = new java.net.URL(pageUrl).getHost();
            String[] pairs = cookieStr.split(";");
            for (String pair : pairs) {
                String trimmed = pair.trim();
                if (!trimmed.isEmpty() && trimmed.contains("=")) {
                    String[] kv = trimmed.split("=", 2);
                    cookies.add(new Cookie(kv[0].trim(), kv[1].trim())
                        .setDomain(domain)
                        .setPath("/"));
                }
            }
        } catch (Exception e) {
            log.warn("Cookie解析失败: {}", e.getMessage());
        }
        return cookies;
    }

    private List<ImageInfo> extractImages(String html, String baseUrl) {
        Set<String> seenUrls = new HashSet<>();
        List<ImageInfo> images = new ArrayList<>();

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
