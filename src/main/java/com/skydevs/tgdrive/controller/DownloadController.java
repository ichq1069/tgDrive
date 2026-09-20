package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.service.DownloadService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/d")
@Slf4j
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;

    @GetMapping("/{fileID}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@NotBlank(message = "fileID不能为空") @PathVariable String fileID) {
        log.info("接收到下载请求，fileID: {}", fileID);
        return downloadService.downloadFile(fileID);
    }
}
