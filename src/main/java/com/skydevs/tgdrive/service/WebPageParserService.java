package com.skydevs.tgdrive.service;

import java.util.List;

public interface WebPageParserService {

    /**
     * 解析网页并提取图片URL列表
     * @param url 网页URL
     * @param cookie 可选的Cookie（用于绕过反爬）
     * @return 图片URL列表
     */
    ParseResult parseWebPage(String url, String cookie);

    /**
     * 解析网页并提取图片URL列表（指定设备模式和CSS选择器）
     * @param url 网页URL
     * @param cookie 可选的Cookie
     * @param cssSelector 可选的CSS选择器，限定提取范围
     * @return 图片URL列表
     */
    ParseResult parseWebPage(String url, String cookie, String cssSelector);

    /**
     * 解析结果
     */
    class ParseResult {
        private boolean success;
        private String title;
        private List<ImageInfo> images;
        private String error;

        public ParseResult() {}

        public ParseResult(boolean success, String title, List<ImageInfo> images, String error) {
            this.success = success;
            this.title = title;
            this.images = images;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<ImageInfo> getImages() { return images; }
        public void setImages(List<ImageInfo> images) { this.images = images; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * 图片/视频信息
     */
    class ImageInfo {
        private String url;
        private String alt;
        private String type;  // "image" or "video"
        private int width;
        private int height;

        public ImageInfo() {}

        public ImageInfo(String url, String alt) {
            this.url = url;
            this.alt = alt;
            this.type = "image";
        }

        public ImageInfo(String url, String alt, String type) {
            this.url = url;
            this.alt = alt;
            this.type = type;
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getAlt() { return alt; }
        public void setAlt(String alt) { this.alt = alt; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }
}
