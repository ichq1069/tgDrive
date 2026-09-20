package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.dto.UrlImportRequest;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ImportService;
import com.skydevs.tgdrive.service.WebPageParserService;
import com.skydevs.tgdrive.service.impl.BrowserPageParserServiceImpl;
import com.skydevs.tgdrive.websocket.ParseProgressWebSocketHandler;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;
    private final WebPageParserService webPageParserService;
    private final BrowserPageParserServiceImpl browserPageParserService;
    private final ParseProgressWebSocketHandler parseProgressWebSocketHandler;

    public ImportController(
            ImportService importService,
            WebPageParserService webPageParserService,
            BrowserPageParserServiceImpl browserPageParserService,
            ParseProgressWebSocketHandler parseProgressWebSocketHandler) {
        this.importService = importService;
        this.webPageParserService = webPageParserService;
        this.browserPageParserService = browserPageParserService;
        this.parseProgressWebSocketHandler = parseProgressWebSocketHandler;
    }

    @SaCheckLogin
    @PostMapping("/url")
    public Result<String> importFromUrl(@RequestBody UrlImportRequest request) {
        if (request.getUrls() == null || request.getUrls().isEmpty()) {
            return Result.error("URL列表不能为空");
        }
        // 异步执行导入任务
        Long userId = StpUtil.getLoginIdAsLong();
        importService.importFromUrls(request.getUrls(), userId, null);
        return Result.success("导入任务已提交，共 " + request.getUrls().size() + " 个文件");
    }

    @SaCheckLogin
    @PostMapping("/url-with-source")
    public Result<String> importFromUrlWithSource(@RequestBody UrlImportRequest request) {
        if (request.getUrls() == null || request.getUrls().isEmpty()) {
            return Result.error("URL列表不能为空");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        importService.importFromUrls(request.getUrls(), userId, request.getSourcePage(), request.getTags(), request.getContentLevel());
        return Result.success("导入任务已提交，共 " + request.getUrls().size() + " 个文件");
    }

    @SaCheckLogin
    @PostMapping("/parse-page")
    public Result<WebPageParserService.ParseResult> parseWebPage(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        String cookie = request.get("cookie");
        String browserMode = request.get("browserMode");
        String deviceMode = request.get("deviceMode");
        String cssSelector = request.get("cssSelector");
        
        if (url == null || url.isEmpty()) {
            return Result.error("URL不能为空");
        }
        
        WebPageParserService.ParseResult result;
        if ("true".equals(browserMode)) {
            result = browserPageParserService.parseWebPage(url, cookie, deviceMode, progress -> {
                parseProgressWebSocketHandler.sendProgress(url, "progress", progress);
            }, cssSelector);
            parseProgressWebSocketHandler.sendProgress(url, "done", "解析完成");
        } else {
            result = webPageParserService.parseWebPage(url, cookie, cssSelector);
        }
        
        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.error(result.getError());
        }
    }

    @SaCheckLogin
    @PostMapping("/parse-and-import")
    public Result<String> parseAndImport(@RequestBody Map<String, Object> request) {
        String url = (String) request.get("url");
        String cookie = (String) request.get("cookie");
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) request.get("imageUrls");
        
        if (url == null || url.isEmpty()) {
            return Result.error("URL不能为空");
        }
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Result.error("图片URL列表不能为空");
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        importService.importFromUrls(imageUrls, userId, url);
        return Result.success("导入任务已提交，共 " + imageUrls.size() + " 个文件，来源页面: " + url);
    }
}
