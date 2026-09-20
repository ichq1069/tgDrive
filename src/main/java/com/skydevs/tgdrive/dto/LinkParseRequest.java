package com.skydevs.tgdrive.dto;

public class LinkParseRequest {
    private String url;
    private String platform;
    private String cookie;
    private String token;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getCookie() { return cookie; }
    public void setCookie(String cookie) { this.cookie = cookie; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
