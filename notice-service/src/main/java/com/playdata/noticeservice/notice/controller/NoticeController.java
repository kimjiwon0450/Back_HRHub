package com.playdata.noticeservice.notice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.auth.CustomUserDetails;
import com.playdata.noticeservice.common.auth.TokenUserInfo;
import com.playdata.noticeservice.common.client.DepartmentClient;
import com.playdata.noticeservice.common.dto.CommonErrorDto;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.*;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.service.NoticeService;

import com.playdata.noticeservice.notice.service.S3Service;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final DepartmentClient departmentClient;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    // 전체글 조회
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

            List<Notice> noticeList = noticeService.getTopNotices(keyword, fromDate, toDate, departmentId);
            Page<Notice> postList = noticeService.getFilteredPosts(keyword, fromDate, toDate, departmentId, pageable);

            log.info("noticeList: {}", noticeList);

            // 🔥 작성자 이름 포함하여 변환
            List<NoticeResponse> noticeDtos = noticeList.stream()
                    .map(notice -> {
                        log.info("notice in stream map: {}", notice);
                        HrUserResponse user = hrUserClient.getUserInfo(notice.getEmployeeId());
                        return NoticeResponse.fromEntity(notice, user.getName());
                    }).toList();

            List<NoticeResponse> postDtos = postList.getContent().stream()
                    .map(notice -> {
                        HrUserResponse user = hrUserClient.getUserInfo(notice.getEmployeeId());
                        return NoticeResponse.fromEntity(notice, user.getName());
                    }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("notices", noticeDtos);
            response.put("posts", postDtos);
            response.put("totalPages", postList.getTotalPages());
            response.put("currentPage", postList.getNumber());

            return ResponseEntity.ok(response);
        }

    // 글 상세 화면 조회
    @GetMapping("/noticeboard/{id}")
    public ResponseEntity<NoticeResponse> getPost(@PathVariable Long id) {
        Notice notice = noticeService.findPostById(id);
        HrUserResponse user = hrUserClient.getUserInfo(notice.getEmployeeId());
        DepResponse dep = departmentClient.getDepInfo(notice.getDepartmentId());
        return ResponseEntity.ok(NoticeResponse.fromEntity(notice, user.getName(), dep.getName()));

    }

    // 글 작성 페이지
    @PostMapping("/noticeboard/write")
    public ResponseEntity<Void> createNotice(
            @RequestBody @Valid NoticeCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) throws IOException {
        Long employeeId = userInfo.getEmployeeId();
        HrUserResponse user = hrUserClient.getUserInfo(employeeId);

        boolean hasAttachment = (files != null && !files.isEmpty());
        request.setHasAttachment(hasAttachment);

        List<String> fileUrls = hasAttachment ? s3Service.uploadFiles(files) : Collections.emptyList();

        noticeService.createNotice(request, employeeId, user.getDepartmentId(), fileUrls);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 글 수정 페이지
    @PutMapping(value = "/noticeboard/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateNotice(
            @PathVariable Long id,
            @RequestBody @Valid NoticeUpdateRequest request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {

        Long employeeId = userInfo.getEmployeeId();
        // 파일이 없기 때문에 null 전달 또는 별도 처리
        noticeService.updateNotice(id, request, null, employeeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/noticeboard/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadFiles(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal TokenUserInfo userInfo) {

        Long employeeId = userInfo.getEmployeeId();
        noticeService.uploadNoticeFiles(id, files, employeeId);
        return ResponseEntity.ok().build();
    }



    // 글 삭제
    @DeleteMapping("/noticeboard/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal TokenUserInfo userInfo) {
        noticeService.deletePost(id, userInfo.getEmployeeId());
        return ResponseEntity.noContent().build();
    }

    // ✅ 공지글 읽음 처리
    @PostMapping("/noticeboard/{id}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal TokenUserInfo userInfo,
                                           @PathVariable Long id) {
        log.info("/noticeboard/{}/read: POST", id);
        log.info("userInfo: {}", userInfo);
        noticeService.markAsRead(userInfo.getEmployeeId(), id);
        return ResponseEntity.ok().build();
    }

    // 내가 쓴 글 조회
    @GetMapping("/noticeboard/my")
    public ResponseEntity<List<NoticeResponse>> getMyPosts(@AuthenticationPrincipal TokenUserInfo userInfo) {
        List<NoticeResponse> notices = noticeService.getMyPosts(userInfo.getEmployeeId());
        return ResponseEntity.ok(notices);
    }

    // 나의 부서글 조회
    @GetMapping("/noticeboard/mydepartment")
    public ResponseEntity<List<NoticeResponse>> getDepartmentPosts(@AuthenticationPrincipal TokenUserInfo userInfo) {
        List<NoticeResponse> notices = noticeService.getDepartmentPosts(userInfo.getEmployeeId());
        return ResponseEntity.ok(notices);
    }


    @GetMapping("/noticeboard/unread-count")
    public ResponseEntity<Integer> getUnreadNoticeCount(@AuthenticationPrincipal TokenUserInfo userInfo) {
        return ResponseEntity.ok(noticeService.getUnreadNoticeCount(userInfo.getEmployeeId(),null,null,null,null));
    }


    // 부서별 조회
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
                    HrUserResponse user = hrUserClient.getUserInfo(n.getEmployeeId());
                    return NoticeResponse.fromEntity(n, user.getName());
                }).toList();

        List<NoticeResponse> postDtos = filteredPosts.getContent().stream()
                .map(n -> {
                    HrUserResponse user = hrUserClient.getUserInfo(n.getEmployeeId());
                    return NoticeResponse.fromEntity(n, user.getName());
                }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("notices", noticeDtos);
        response.put("posts", postDtos);
        response.put("totalPages", filteredPosts.getTotalPages());
        response.put("currentPage", filteredPosts.getNumber());

        return ResponseEntity.ok(response);
    }

    // 👉 추후 기타 알림 (ex: 전자결재, 일정 알림 등) 도 여기에 추가할 수 있음.
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, List<NoticeResponse>>> getUserAlerts(
            @RequestParam Long userId
    ) {
        Map<String, List<NoticeResponse>> result = noticeService.getUserAlerts(userId);
        return ResponseEntity.ok(result);
    }

}