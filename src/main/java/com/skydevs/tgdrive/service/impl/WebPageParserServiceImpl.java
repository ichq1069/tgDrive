package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.service.WebPageParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class WebPageParserServiceImpl implements WebPageParserService {

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    @Override
    public ParseResult parseWebPage(String pageUrl, String cookie) {
        try {
            // 下载网页内容
            String html = downloadPage(pageUrl, cookie);
            if (html == null || html.isEmpty()) {
                return new ParseResult(false, null, null, "无法获取网页内容");
            }

            // 提取页面标题
            String title = extractTitle(html);

            // 提取所有图片URL
            List<ImageInfo> images = extractImages(html, pageUrl);

            log.info("网页解析完成: URL={}, 标题={}, 图片数={}", pageUrl, title, images.size());
            return new ParseResult(true, title, images, null);

        } catch (Exception e) {
            log.error("网页解析失败: {} -> {}", pageUrl, e.getMessage());
            return new ParseResult(false, null, null, "解析失败: " + e.getMessage());
        }
    }

    private String downloadPage(String pageUrl, String cookie) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(pageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setInstanceFollowRedirects(true);

            // 添加Cookie（如果提供）
            if (cookie != null && !cookie.isEmpty()) {
                conn.setRequestProperty("Cookie", cookie);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP " + responseCode);
            }

            // 检测编码
            String contentType = conn.getContentType();
            String charset = "UTF-8";
            if (contentType != null) {
                Pattern charsetPattern = Pattern.compile("charset=([\\w-]+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = charsetPattern.matcher(contentType);
                if (matcher.find()) {
                    charset = matcher.group(1);
                }
            }

            try (InputStream is = conn.getInputStream();
                 java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
                byte[] data = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
                return buffer.toString(charset);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String extractTitle(String html) {
        Pattern pattern = Pattern.compile("<title[^>]*>([^<]*)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private List<ImageInfo> extractImages(String html, String baseUrl) {
        Set<String> seenUrls = new HashSet<>();
        List<ImageInfo> images = new ArrayList<>();

        // 1. 提取 <img> 标签的 src 和 data-src（懒加载）
        Pattern imgPattern = Pattern.compile(
            "<img[^>]+(?:data-src|data-original|data-lazy-src|src)\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher imgMatcher = imgPattern.matcher(html);
        while (imgMatcher.find()) {
            String imgUrl = resolveUrl(imgMatcher.group(1), baseUrl);
            if (imgUrl != null && !seenUrls.contains(imgUrl)) {
                seenUrls.add(imgUrl);
                // 提取alt属性
                String alt = extractAttr(imgMatcher.group(0), "alt");
                images.add(new ImageInfo(imgUrl, alt));
            }
        }

        // 2. 提取 CSS background-image
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

        // 3. 提取 srcset（响应式图片）
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

        // 4. 提取 <picture> <source> 标签
        Pattern sourcePattern = Pattern.compile(
            "<source[^>]+srcset\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher sourceMatcher = sourcePattern.matcher(html);
        while (sourceMatcher.find()) {
            String srcset = sourceMatcher.group(1);
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

        // 5. 提取 meta og:image
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

        // 跳过data: URI和javascript:
        if (url.startsWith("data:") || url.startsWith("javascript:") || url.startsWith("#")) {
            return null;
        }

        // 已经是完整URL
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        // 相对URL
        try {
            URL base = new URL(baseUrl);
            URL resolved = new URL(base, url);
            return resolved.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
