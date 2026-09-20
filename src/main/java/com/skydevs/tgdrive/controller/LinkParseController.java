package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.dto.LinkParseRequest;
import cn.dev33.satoken.annotation.SaCheckLogin;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

@RestController
@RequestMapping("/api/link-parser")
public class LinkParseController {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @PostMapping("/parse")
    @SaCheckLogin
    public Map<String, Object> parseLink(@RequestBody LinkParseRequest req) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = req.getUrl();
            if (url == null || url.isBlank()) {
                result.put("code", 0);
                result.put("message", "URL不能为空");
                return result;
            }

            String platform = req.getPlatform() != null ? req.getPlatform() : detectPlatform(url);
            List<Map<String, String>> items = new ArrayList<>();

            switch (platform) {
                case "instagram":
                    items = parseInstagram(url, req.getCookie());
                    break;
                case "twitter":
                    items = parseTwitter(url, req.getCookie());
                    break;
                case "youtube":
                    items = parseYouTube(url);
                    break;
                case "tiktok":
                    items = parseTikTok(url, req.getCookie());
                    break;
                case "xiaohongshu":
                    items = parseXiaohongshu(url, req.getCookie());
                    break;
                default:
                    items = parseGeneric(url, req.getCookie());
                    break;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            data.put("platform", platform);
            result.put("code", 1);
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 0);
            result.put("message", "解析失败: " + e.getMessage());
        }
        return result;
    }

    private String detectPlatform(String url) {
        if (url.matches("(?i).*instagram\\.com.*")) return "instagram";
        if (url.matches("(?i).*(twitter\\.com|x\\.com).*")) return "twitter";
        if (url.matches("(?i).*youtube\\.com.*|youtu\\.be.*")) return "youtube";
        if (url.matches("(?i).*tiktok\\.com.*")) return "tiktok";
        if (url.matches("(?i).*xiaohongshu\\.com.*|xhslink\\.com.*")) return "xiaohongshu";
        return "generic";
    }

    private List<Map<String, String>> parseInstagram(String url, String cookie) {
        List<Map<String, String>> items = new ArrayList<>();
        try {
            String html = fetchHtml(url, cookie);
            if (html == null) return items;

            Pattern p = Pattern.compile("\"display_url\":\"(https?://[^\"]+)\"");
            Matcher m = p.matcher(html);
            while (m.find()) {
                String imgUrl = m.group(1).replace("\\u0026", "&");
                Map<String, String> item = new HashMap<>();
                item.put("url", imgUrl);
                item.put("type", "image");
                item.put("filename", extractFilename(imgUrl));
                items.add(item);
            }

            Pattern vp = Pattern.compile("\"video_url\":\"(https?://[^\"]+)\"");
            Matcher vm = vp.matcher(html);
            while (vm.find()) {
                String videoUrl = vm.group(1).replace("\\u0026", "&");
                Map<String, String> item = new HashMap<>();
                item.put("url", videoUrl);
                item.put("type", "video");
                item.put("filename", extractFilename(videoUrl));
                items.add(item);
            }
        } catch (Exception e) {
            // ignore parse errors
        }
        return items;
    }

    private List<Map<String, String>> parseTwitter(String url, String cookie) {
        List<Map<String, String>> items = new ArrayList<>();
        try {
            String html = fetchHtml(url, cookie);
            if (html == null) return items;

            Pattern p = Pattern.compile("\"url\":\"(https?://pbs\\.twimg\\.com/media/[^\"]+)\"");
            Matcher m = p.matcher(html);
            while (m.find()) {
                String imgUrl = m.group(1).replace("\\u0026", "&");
                Map<String, String> item = new HashMap<>();
                item.put("url", imgUrl);
                item.put("type", "image");
                item.put("filename", extractFilename(imgUrl));
                items.add(item);
            }

            Pattern vp = Pattern.compile("\"url\":\"(https?://video\\.twimg\\.com/[^\"]+)\"");
            Matcher vm = vp.matcher(html);
            while (vm.find()) {
                String videoUrl = vm.group(1).replace("\\u0026", "&");
                Map<String, String> item = new HashMap<>();
                item.put("url", videoUrl);
                item.put("type", "video");
                item.put("filename", extractFilename(videoUrl));
                items.add(item);
            }
        } catch (Exception e) {
            // ignore
        }
        return items;
    }

    private List<Map<String, String>> parseYouTube(String url) {
        List<Map<String, String>> items = new ArrayList<>();
        try {
            String videoId = null;
            Matcher m = Pattern.compile("(?:v=|youtu\\.be/)([a-zA-Z0-9_-]{11})").matcher(url);
            if (m.find()) videoId = m.group(1);

            if (videoId != null) {
                String thumbnail = "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";
                Map<String, String> item = new HashMap<>();
                item.put("url", thumbnail);
                item.put("thumbnail", thumbnail);
                item.put("type", "image");
                item.put("filename", videoId + ".jpg");
                items.add(item);
            }
        } catch (Exception e) {
            // ignore
        }
        return items;
    }

    private List<Map<String, String>> parseTikTok(String url, String cookie) {
        List<Map<String, String>> items = new ArrayList<>();
        try {
            String html = fetchHtml(url, cookie);
            if (html == null) return items;

            Pattern p = Pattern.compile("\"playAddr\":\"(https?://[^\"]+)\"");
            Matcher m = p.matcher(html);
            while (m.find()) {
                String videoUrl = m.group(1).replace("\\u002F", "/");
                Map<String, String> item = new HashMap<>();
                item.put("url", videoUrl);
                item.put("type", "video");
                item.put("filename", "tiktok_" + System.currentTimeMillis() + ".mp4");
                items.add(item);
            }

            Pattern ip = Pattern.compile("\"cover\":\"(https?://[^\"]+)\"");
            Matcher im = ip.matcher(html);
            if (im.find()) {
                Map<String, String> thumbItem = new HashMap<>();
                thumbItem.put("url", im.group(1).replace("\\u002F", "/"));
                thumbItem.put("type", "image");
                thumbItem.put("filename", "tiktok_cover.jpg");
                items.add(thumbItem);
            }
        } catch (Exception e) {
            // ignore
        }
        return items;
    }

    private List<Map<String, String>> parseXiaohongshu(String url, String cookie) {
        List<Map<String, String>> items = new ArrayList<>();
        try {
            String html = fetchHtml(url, cookie);
            if (html == null) return items;

            Pattern p = Pattern.compile("\"url\":\"(https?://[^\"]*?(?:sns-webpic|ci\\.xiaohongshu|sns-img)[^\"]+)\"");
            Matcher m = p.matcher(html);
            while (m.find()) {
                String imgUrl = m.group(1).replace("\\u0026", "&");
                Map<String, String> item = new HashMap<>();
                item.put("url", imgUrl);
                item.put("type", "image");
                item.put("filename", extractFilename(imgUrl));
                items.add(item);
            }

            Pattern vp = Pattern.compile("\"url\":\"(https?://[^\"]*?video[^\"]+\\.mp4[^\"]*)\"");
            Matcher vm = vp.matcher(html);
            while (vm.find()) {
                String videoUrl = vm.group(1).replace("\\u0026", "&");
                Map<String, String> item = new HashMap<>();
                item.put("url", videoUrl);
                item.put("type", "video");
                item.put("filename", extractFilename(videoUrl));
                items.add(item);
            }
        } catch (Exception e) {
            // ignore
        }
        return items;
    }

    private List<Map<String, String>> parseGeneric(String url, String cookie) {
        List<Map<String, String>> items = new ArrayList<>();
        try {
            String html = fetchHtml(url, cookie);
            if (html == null) return items;

            Document doc = Jsoup.parse(html);
            Elements imgs = doc.select("img[src]");
            for (Element img : imgs) {
                String src = img.attr("abs:src");
                if (src.isEmpty()) src = img.attr("src");
                if (src.startsWith("http")) {
                    Map<String, String> item = new HashMap<>();
                    item.put("url", src);
                    item.put("type", "image");
                    item.put("filename", extractFilename(src));
                    items.add(item);
                }
            }

            Elements videos = doc.select("video source[src], video[src]");
            for (Element v : videos) {
                String src = v.attr("abs:src");
                if (src.isEmpty()) src = v.attr("src");
                if (src.startsWith("http")) {
                    Map<String, String> item = new HashMap<>();
                    item.put("url", src);
                    item.put("type", "video");
                    item.put("filename", extractFilename(src));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return items;
    }

    private String fetchHtml(String url, String cookie) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9");
            if (cookie != null && !cookie.isBlank()) {
                builder.header("Cookie", cookie);
            }
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFilename(String url) {
        try {
            String path = new URI(url).getPath();
            if (path != null) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (!name.isEmpty()) return name;
            }
        } catch (Exception e) {
            // ignore
        }
        return "file_" + System.currentTimeMillis();
    }
}
