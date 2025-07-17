package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.service.AttachmentService;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/approval/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/preview")
    public ResponseEntity<Void> previewAttachment(
            @RequestParam("reportId") Long reportId,
            @RequestParam("fileUrl") String fileUrl,
            @AuthenticationPrincipal TokenUserInfo userInfo) {

        String presignedUrl = attachmentService.getPresignedUrlForAction(reportId, fileUrl, userInfo.getEmail(), "inline");

        // 브라우저에게 'presignedUrl'로 이동하라는 302 리다이렉트 응답을 보냅니다.
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }

    @GetMapping("/download")
    public ResponseEntity<Void> downloadAttachment(
            @RequestParam("reportId") Long reportId,
            @RequestParam("fileUrl") String fileUrl,
            @AuthenticationPrincipal TokenUserInfo userInfo) {

        String presignedUrl = attachmentService.getPresignedUrlForAction(reportId, fileUrl, userInfo.getEmail(), "attachment");

        // 브라우저에게 'presignedUrl'로 이동하라는 302 리다이렉트 응답을 보냅니다.
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }
}