package com.playdata.noticeservice.notice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.auth.TokenUserInfo;
import com.playdata.noticeservice.common.client.DepartmentClient;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.dto.DepResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
@RequestMapping("/notice")
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
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        log.info("~~~게시글 조회 페이지 진입함~~~");
        log.info("sortBy: {}, desc: {}", sortBy, sortDir);
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        boolean hasFilter = !((keyword == null || keyword.isBlank()) && fromDate == null && toDate == null);

        List<Notice> topGeneralNotices;
        List<Notice> topNotices;
        Page<Notice> posts;

        if (hasFilter) {
            topGeneralNotices = noticeService.getFilteredGeneralNotices(keyword, fromDate, toDate, pageSize, sortBy, sortDir);
            topNotices = noticeService.getFilteredNotices(keyword, fromDate, toDate, pageSize, sortBy, sortDir);
            posts = noticeService.getFilteredPosts(page, keyword, fromDate, toDate, pageSize, sortBy, sortDir);
        } else {
            // 부서 전체 공지글 5개
            topGeneralNotices = noticeService.getGeneralNotices().stream().limit(5).toList();
            // 상위 공지글 5개
            topNotices = noticeService.getAllNotices(sortBy, sortDir).stream().limit(5).toList();
            // 나머지 공지글 + 일반글 필터링한 결과를 수동 페이징 처리
            posts = noticeService.getMergedPostsAfterTop5(page, pageSize, sortBy, sortDir);
        }

        Set<Long> employeeIds = Stream.concat(Stream.concat(topGeneralNotices.stream(), topNotices.stream()), posts.stream())
                .map(Notice::getEmployeeId)
                .collect(Collectors.toSet());

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(employeeIds).stream()
                .collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();
        response.put("generalNotices", topGeneralNotices.stream().map(n -> NoticeResponse.fromEntity(n, userMap.get(n.getEmployeeId()))).toList());
        response.put("notices", topNotices.stream().map(n -> NoticeResponse.fromEntity(n, userMap.get(n.getEmployeeId()))).toList());
        response.put("posts", posts.stream().map(n -> NoticeResponse.fromEntity(n, userMap.get(n.getEmployeeId()))).toList());

        response.put("totalPages", posts.getTotalPages());
        response.put("currentPage", posts.getNumber());
        log.info("response 결과 확인");
        log.info(response.toString());

        return ResponseEntity.ok(response);
    }

    // 내가 쓴 글 조회
    @GetMapping("/noticeboard/my")
    public ResponseEntity<List<NoticeResponse>> getMyPosts(@AuthenticationPrincipal TokenUserInfo userInfo) {
        List<Notice> notices = noticeService.getMyPosts(userInfo.getEmployeeId());

//        List<NoticeResponse> responseList = notices.stream()
//                .map(notice -> {
//                    HrUserResponse user = hrUserClient.getUserInfo(notice.getEmployeeId());
//                    return NoticeResponse.fromEntity(notice, user);
//                })
//                .toList();
//
//        return ResponseEntity.ok(responseList);

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(
                notices.stream().map(Notice::getEmployeeId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        return ResponseEntity.ok(
                notices.stream().map(n -> NoticeResponse.fromEntity(n, userMap.get(n.getEmployeeId()))).toList()
        );
    }

    // 전체 공지 조회 (department_id = 0)
    @GetMapping("/noticeboard/generalnotice")
    public ResponseEntity<List<NoticeResponse>> getGeneralNotices(@AuthenticationPrincipal TokenUserInfo userInfo) {
        List<Notice> notices = noticeService.getGeneralNotices();

        List<NoticeResponse> responseList = notices.stream()
                .map(notice -> {
                    HrUserResponse user = hrUserClient.getUserInfo(notice.getEmployeeId());
                    return NoticeResponse.fromEntity(notice, user);
                })
                .toList();

        return ResponseEntity.ok(responseList);
    }



    // 나의 부서글 조회
    @GetMapping("/noticeboard/mydepartment")
    public ResponseEntity<List<NoticeResponse>> getDepartmentPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int pageSize,
            @AuthenticationPrincipal TokenUserInfo userInfo) {


        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Notice> notices = noticeService.getNoticesByDepartment(userInfo.getDepartmentId(), keyword, fromDate, toDate);
        List<Notice> posts = noticeService.getPostsByDepartment(userInfo.getDepartmentId(), keyword, fromDate, toDate, pageable);

//        List<NoticeResponse> responseList = Stream.concat(notices.stream(), posts.stream())
//                .map(notice -> {
//                    HrUserResponse writer = hrUserClient.getUserInfo(notice.getEmployeeId());
//                    return NoticeResponse.fromEntity(notice, writer);
//                })
//                .toList();
//
//        return ResponseEntity.ok(responseList);

        List<Notice> combined = Stream.concat(notices.stream(), posts.stream()).toList();
        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(
                combined.stream().map(Notice::getEmployeeId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        return ResponseEntity.ok(
                combined.stream().map(n -> NoticeResponse.fromEntity(n, userMap.get(n.getEmployeeId()))).toList()
        );
    }

    // 글 상세 화면 조회
    @GetMapping("/noticeboard/{id}")
    public ResponseEntity<NoticeResponse> getPost(@PathVariable Long id) {
        Notice notice = noticeService.findPostById(id);
        HrUserResponse user = hrUserClient.getUserInfo(notice.getEmployeeId());
        DepResponse dep = departmentClient.getDepInfo(notice.getDepartmentId());
        return ResponseEntity.ok(NoticeResponse.fromEntity(notice, user, dep));
    }

    // 글 작성 페이지
    @PostMapping("/noticeboard/write")
    public ResponseEntity<Void> createNotice(
            @RequestBody @Valid NoticeCreateRequest request,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) throws IOException {
        Long employeeId = userInfo.getEmployeeId();
        HrUserResponse user = hrUserClient.getUserInfo(employeeId);

        // ✅ attachmentUri를 List<String>으로 변환
        List<String> attachmentUri = Collections.emptyList();
        if (request.getAttachmentUri() != null && !request.getAttachmentUri().isBlank()) {
            attachmentUri = new ObjectMapper().readValue(request.getAttachmentUri(), new TypeReference<>() {});
        }

        // ✅ 실제 서비스 호출
        noticeService.createNotice(request, employeeId, attachmentUri);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @GetMapping("/noticeboard/upload-url")
    public ResponseEntity<String> generateUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        String url = s3Service.generatePresignedUrlForPut(fileName, contentType);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/noticeboard/download-url")
    public ResponseEntity<String> generateDownloadUrl(@RequestParam String fileName) {
        String url = s3Service.generatePresignedUrlForGet(fileName, "application/octet-stream");
        return ResponseEntity.ok(url);
    }


    // 글 수정 페이지
    @PutMapping(value = "/noticeboard/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateNotice(
            @PathVariable Long id,
            @RequestBody @Valid NoticeUpdateRequest request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {

        Long employeeId = userInfo.getEmployeeId();
        // 파일이 없기 때문에 null 전달 또는 별도 처리
        noticeService.updateNotice(id, request, employeeId);
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


    // 읽지 않은 공지글 카운트
    @GetMapping("/noticeboard/unread-count")
    public ResponseEntity<Integer> getUnreadNoticeCount(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        Long userId = userInfo.getEmployeeId();
        HrUserResponse user = hrUserClient.getUserInfo(userId);
        int count = noticeService.countUnreadNotices(userId, user.getDepartmentId());
        return ResponseEntity.ok(count);
    }


    // 👉 추후 기타 알림 (ex: 전자결재, 일정 알림 등) 도 여기에 추가할 수 있음.
    @GetMapping("/noticeboard/alerts")
    public ResponseEntity<Map<String, List<NoticeResponse>>> getUserAlerts(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        Long userId = userInfo.getEmployeeId();
        HrUserResponse user = hrUserClient.getUserInfo(userId);
        Map<String, List<NoticeResponse>> result = noticeService.getUserAlerts(userId, user.getDepartmentId());
        return ResponseEntity.ok(result);
    }

    ///////////////////////////댓글 Controller//////////////////////////////

    // ✅ 댓글 작성
    @PostMapping("/noticeboard/{noticeId}/comments")
    public ResponseEntity<Void> createComment(@PathVariable Long noticeId,
                                              @RequestBody @Valid CommentCreateRequest request,
                                              @AuthenticationPrincipal TokenUserInfo userInfo) {
        noticeService.createComment(noticeId, request, userInfo.getEmployeeId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ✅ 댓글 목록 조회
    @GetMapping("/noticeboard/{noticeId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long noticeId) {
        List<CommentResponse> comments = noticeService.getComments(noticeId);
        return ResponseEntity.ok(comments);
    }

    // ✅ 댓글 수정
    @PutMapping("/noticeboard/{noticeId}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(@PathVariable Long noticeId,
                                              @PathVariable Long commentId,
                                              @RequestBody @Valid CommentUpdateRequest request,
                                              @AuthenticationPrincipal TokenUserInfo userInfo) {
        noticeService.updateComment(noticeId, commentId, request, userInfo.getEmployeeId());
        return ResponseEntity.ok().build();
    }

    // ✅ 댓글 삭제
    @DeleteMapping("/noticeboard/{noticeId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long noticeId,
                                              @PathVariable Long commentId,
                                              @AuthenticationPrincipal TokenUserInfo userInfo) {
        noticeService.deleteComment(noticeId, commentId, userInfo.getEmployeeId());
        return ResponseEntity.noContent().build();
    }

}