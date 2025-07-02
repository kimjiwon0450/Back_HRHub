package com.playdata.noticeservice.notice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.auth.CustomUserDetails;
import com.playdata.noticeservice.common.dto.CommonErrorDto;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.*;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.service.NoticeService;

import com.playdata.noticeservice.notice.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/notice-service")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final Environment env;
    private final NoticeService noticeService;
    private final HrUserClient hrUserClient;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    @GetMapping("/noticeboard")
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
        ) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            List<Notice> noticeList = noticeService.getTopNotices();
            Page<Notice> postList = noticeService.getFilteredPosts(keyword, fromDate, toDate, departmentId, pageable);

            // 🔥 작성자 이름 포함하여 변환
            List<NoticeResponse> noticeDtos = noticeList.stream()
                    .map(notice -> {
                        HrUserResponse user = hrUserClient.getUserInfo(notice.getWriterId());
                        return NoticeResponse.fromEntity(notice, user.getUsername());
                    }).toList();

            List<NoticeResponse> postDtos = postList.getContent().stream()
                    .map(notice -> {
                        HrUserResponse user = hrUserClient.getUserInfo(notice.getWriterId());
                        return NoticeResponse.fromEntity(notice, user.getUsername());
                    }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("notices", noticeDtos);
            response.put("posts", postDtos);
            response.put("totalPages", postList.getTotalPages());
            response.put("currentPage", postList.getNumber());

            return ResponseEntity.ok(response);
        }

    @GetMapping("/noticeboard/{id}")
    public ResponseEntity<NoticeResponse> getPost(@PathVariable Long id) {
        Notice notice = noticeService.findPostById(id);
        HrUserResponse user = hrUserClient.getUserInfo(notice.getWriterId());
        return ResponseEntity.ok(NoticeResponse.fromEntity(notice, user.getUsername()));
    }

    @PostMapping("/noticeboard/write")
    public ResponseEntity<Void> createNotice(
            @RequestPart("data") NoticeCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        Long userId = userDetails.getId();
        HrUserResponse user = hrUserClient.getUserInfo(userId);

        boolean hasAttachment = (files != null && !files.isEmpty());
        request.setHasAttachment(hasAttachment);

        List<String> fileUrls = hasAttachment ? s3Service.uploadFiles(files) : Collections.emptyList();

        noticeService.createNotice(request, userId, user.getDepartmentId(), fileUrls);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/noticeboard/{id}")
    public ResponseEntity<Void> updateNotice(@PathVariable Long id,
                                             @RequestBody @Valid NoticeUpdateRequest request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        noticeService.updateNotice(id, request, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/noticeboard/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        noticeService.deletePost(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // ✅ 공지글 읽음 처리
    @PostMapping("/noticeboard/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,
                                           Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        noticeService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/noticeboard/my")
    public ResponseEntity<List<NoticeResponse>> getMyPosts(@AuthenticationPrincipal(expression = "id") Long userId) {
        List<NoticeResponse> notices = noticeService.getMyPosts(userId);
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/noticeboard/unread-count")
    public ResponseEntity<Integer> getUnreadNoticeCount(@AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(noticeService.getUnreadNoticeCount(userId));
    }

    @GetMapping("/noticeboard/department/{departmentId}")
    public ResponseEntity<Map<String, Object>> getPostsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        List<Notice> topNotices = noticeService.getTopNoticesByDepartment(departmentId);
        Page<Notice> filteredPosts = noticeService.getPostsByDepartment(departmentId, keyword, fromDate, toDate, pageable);

        List<NoticeResponse> noticeDtos = topNotices.stream()
                .map(n -> {
                    HrUserResponse user = hrUserClient.getUserInfo(n.getWriterId());
                    return NoticeResponse.fromEntity(n, user.getUsername());
                }).toList();

        List<NoticeResponse> postDtos = filteredPosts.getContent().stream()
                .map(n -> {
                    HrUserResponse user = hrUserClient.getUserInfo(n.getWriterId());
                    return NoticeResponse.fromEntity(n, user.getUsername());
                }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("notices", noticeDtos);
        response.put("posts", postDtos);
        response.put("totalPages", filteredPosts.getTotalPages());
        response.put("currentPage", filteredPosts.getNumber());

        return ResponseEntity.ok(response);
    }

}