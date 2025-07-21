package com.playdata.noticeservice.notice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.global.dto.AlertResponse;
import com.playdata.global.enums.AlertMessage;
import com.playdata.noticeservice.common.auth.TokenUserInfo;
import com.playdata.noticeservice.common.client.DepartmentClient;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.*;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.Position;

import com.playdata.noticeservice.notice.service.NoticeService_v2;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
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
    private final NoticeService_v2 noticeService;
    private final HrUserClient hrUserClient;
    private final DepartmentClient departmentClient;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    // 전체글 조회
//    @GetMapping("/noticeboard")
//    public ResponseEntity<Map<String, Object>> getAllPosts(
//            @RequestParam(defaultValue = "") String keyword,
//            @RequestParam(required = false)
//            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
//            @RequestParam(required = false)
//            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int pageSize,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @RequestParam(defaultValue = "desc") String sortDir,
//            HttpServletRequest request
//    ) {
//        if (keyword != null && keyword.isBlank()) {
//            keyword = null;
//        }
//
//        String token = request.getHeader("Authorization");  // 토큰 꺼내기
//
//        log.info("~~~게시글 조회 페이지 진입함~~~");
//        log.info("sortBy: {}, desc: {}", sortBy, sortDir);
//        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
//        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
//
//        boolean hasFilter = !((keyword == null || keyword.isBlank()) && fromDate == null && toDate == null);
//
//        List<Notice> topGeneralNotices;
//        List<Notice> topNotices;
//        Page<Notice> posts;
//
//        if (hasFilter) {
//            topGeneralNotices = noticeService.getFilteredGeneralNotices(keyword, fromDate, toDate, pageSize, sortBy, sortDir);
//            topNotices = noticeService.getFilteredNotices(keyword, fromDate, toDate, pageSize, sortBy, sortDir);
//            posts = noticeService.getFilteredPosts(page, keyword, fromDate, toDate, pageSize, sortBy, sortDir);
//        } else {
//            // 부서 전체 공지글 5개
//            topGeneralNotices = noticeService.getGeneralNotices().stream().limit(5).toList();
//            // 상위 공지글 5개
//            topNotices = noticeService.getAllNotices(sortBy, sortDir).stream().limit(5).toList();
//            // 나머지 공지글 + 일반글 필터링한 결과를 수동 페이징 처리
//            posts = noticeService.getMergedPostsAfterTop5(page, pageSize, sortBy, sortDir);
//        }
//
//        Set<Long> employeeIds = Stream.concat(Stream.concat(topGeneralNotices.stream(), topNotices.stream()), posts.stream())
//                .map(Notice::getEmployeeId)
//                .collect(Collectors.toSet());
//
//        log.info("employeeIds : {}",employeeIds);
//        log.info("token : {}",token);
//        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(employeeIds, token).stream()
//                .collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));
//        log.info("userMap : {}",userMap);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("generalNotices", topGeneralNotices.stream()
//                .map(n -> {
//                    HrUserResponse user = userMap.get(n.getEmployeeId());
//                    log.info("user : {}",user);
//                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId());
//                    return NoticeResponse.fromEntity(n, user, commentCount);
//                }).toList());
//
//        response.put("notices", topNotices.stream()
//                .map(n -> {
//                    HrUserResponse user = userMap.get(n.getEmployeeId());
//                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId());
//                    return NoticeResponse.fromEntity(n, user, commentCount);
//                }).toList());
//
//        response.put("posts", posts.stream()
//                .map(n -> {
//                    HrUserResponse user = userMap.get(n.getEmployeeId());
//                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId()); // ✅ 댓글 수
//                    return NoticeResponse.fromEntity(n, user, commentCount); // ✅ 댓글 수 포함
//                }).toList());
//
//        response.put("totalPages", posts.getTotalPages());
//        response.put("currentPage", posts.getNumber());
//        log.info("response 결과 확인");
//        log.info(response.toString());
//
//        return ResponseEntity.ok(response);
//    }

    // ---------------------- 공지사항 API (직급별 필터링) ----------------------

    @GetMapping("/general")
    public ResponseEntity<Map<String, Object>> getGeneralNoticesByPosition(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {

        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        String token = request.getHeader("Authorization");  // 토큰 꺼내기

        boolean hasFilter = !((keyword == null || keyword.isBlank()) && fromDate == null && toDate == null);
        List<Notice> generalNotices;
        Page<Notice> depNotices;

        // 1. 사용자 직급 ID 조회 (직급 정보가 TokenUserInfo에 없으면 HrUserClient로 조회)
        HrUserResponse user = hrUserClient.getUserInfo(userInfo.getEmployeeId());
        Position position = user.getPosition();  // 직급 ID

        if (hasFilter) {
            generalNotices = noticeService.getFilteredGeneralNotices(position, user.getDepartmentId(), keyword, fromDate, toDate, page, pageSize, sortBy, sortDir);
            depNotices = noticeService.getFilteredDepartmentNotices(position, keyword, fromDate, toDate, user.getDepartmentId(), page, pageSize, sortBy, sortDir);
        } else {
            // 부서 전체 공지글 5개
            generalNotices = noticeService.getTopGeneralNotices(sortBy, sortDir, position).stream().limit(5).toList();
            // 부서 공지글
            depNotices = noticeService.getMyDepartmentNotices(position, user.getDepartmentId(), page, pageSize, sortBy, sortDir);
        }

        Set<Long> employeeIds = Stream.concat(generalNotices.stream(), depNotices.stream())
                .map(Notice::getEmployeeId)
                .collect(Collectors.toSet());

        // 3. 작성자 정보 매핑
        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(employeeIds, token)
                .stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();
        response.put("generalNotices", generalNotices.stream()
                .map(n -> {
                    HrUserResponse writer = userMap.get(n.getEmployeeId());
                    log.info("user : {}",writer);
                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId());
                    return NoticeResponse.fromEntity(n, writer, commentCount);
                }).toList());

        response.put("notices", depNotices.stream()
                .map(n -> {
                    HrUserResponse writer = userMap.get(n.getEmployeeId());
                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId());
                    return NoticeResponse.fromEntity(n, writer, commentCount);
                }));

        response.put("totalPages", depNotices.getTotalPages());
        response.put("currentPage", depNotices.getNumber());
        return ResponseEntity.ok(response);
    }


    // ---------------------- 일반 게시글(커뮤니티) API ----------------------

    @GetMapping("/community")
    public ResponseEntity<Map<String, Object>> getCommunityPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        String token = request.getHeader("Authorization");  // 토큰 꺼내기

        boolean hasFilter = !((keyword == null || keyword.isBlank()) && fromDate == null && toDate == null);

        Page<Notice> posts;
        if (hasFilter) {
            posts = noticeService.getFilteredPosts(keyword, fromDate, toDate, page, pageSize, sortBy, sortDir);
        } else {
            posts = noticeService.getAllPosts(page, pageSize, sortBy, sortDir);
        }

        Set<Long> employeeIds = posts.stream()
                .map(Notice::getEmployeeId)
                .collect(Collectors.toSet());

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(employeeIds, token)
                .stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts.stream()
                .map(n -> {
                    HrUserResponse writer = userMap.get(n.getEmployeeId());
                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId());
                    return NoticeResponse.fromEntity(n, writer, commentCount);
                }).toList());
        response.put("totalPages", posts.getTotalPages());
        response.put("currentPage", posts.getNumber());

        return ResponseEntity.ok(response);
    }


    // 내가 쓴 글 조회(공지글)
    @GetMapping("/general/my")
    public ResponseEntity<Map<String, Object>> getMyNotice(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            HttpServletRequest request) {
        List<Notice> notices = noticeService.getMyNotices(userInfo.getEmployeeId());

        String token = request.getHeader("Authorization");

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(
                notices.stream().map(Notice::getEmployeeId).collect(Collectors.toSet())
        , token).stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();

        response.put("mynotices", notices.stream()
                .map(n -> {
                    HrUserResponse user = userMap.get(n.getEmployeeId());
                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId()); // ✅ 댓글 수
                    return NoticeResponse.fromEntity(n, user, commentCount); // ✅ 댓글 수 포함
                }).toList());
        return ResponseEntity.ok(response);
    }

    // 내가 쓴 글 조회(일반글)
    @GetMapping("/community/my")
    public ResponseEntity<Map<String, Object>> getMyPosts(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            HttpServletRequest request) {
        List<Notice> notices = noticeService.getMyPosts(userInfo.getEmployeeId());

        String token = request.getHeader("Authorization");

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(
                notices.stream().map(Notice::getEmployeeId).collect(Collectors.toSet())
                , token).stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();

        response.put("myposts", notices.stream()
                .map(n -> {
                    HrUserResponse user = userMap.get(n.getEmployeeId());
                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId()); // ✅ 댓글 수
                    return NoticeResponse.fromEntity(n, user, commentCount); // ✅ 댓글 수 포함
                }).toList());
        return ResponseEntity.ok(response);
    }


    // 일반게시글 나의 부서글 조회()
    @GetMapping("/noticeboard/mydepartment")
    public ResponseEntity<Map<String, Object>> getDepartmentPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int pageSize,
            @AuthenticationPrincipal TokenUserInfo userInfo,
            HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Notice> posts = noticeService.getMyDepartmentPosts(keyword, fromDate, toDate, userInfo.getDepartmentId(),pageable);

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(
                posts.stream().map(Notice::getEmployeeId).collect(Collectors.toSet())
        , token).stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();

        response.put("mydepposts", posts.stream()
                .map(n -> {
                    HrUserResponse user = userMap.get(n.getEmployeeId());
                    int commentCount = noticeService.getCommentCountByNoticeId(n.getId()); // ✅ 댓글 수
                    return NoticeResponse.fromEntity(n, user, commentCount); // ✅ 댓글 수 포함
                }).toList());
        return ResponseEntity.ok(response);
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
    public ResponseEntity<AlertResponse> createNotice(
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
        noticeService.createNotice(request, user, attachmentUri);
        return ResponseEntity.ok(new AlertResponse(AlertMessage.NOTICE_CREATE_SUCCESS.getMessage(), "success"));
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
    public ResponseEntity<AlertResponse> updateNotice(
            @PathVariable Long id,
            @RequestBody @Valid NoticeUpdateRequest request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {

        Long employeeId = userInfo.getEmployeeId();
        HrUserResponse user = hrUserClient.getUserInfo(employeeId);
        // 파일이 없기 때문에 null 전달 또는 별도 처리
        noticeService.updateNotice(id, request, user);
        return ResponseEntity.ok(new AlertResponse(AlertMessage.NOTICE_UPDATE_SUCCESS.getMessage(), "success"));
    }


    // 글 삭제
    @DeleteMapping("/noticeboard/{id}")
    public ResponseEntity<AlertResponse> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal TokenUserInfo userInfo) {
        noticeService.deletePost(id, userInfo.getEmployeeId());
//        return ResponseEntity.noContent().build();
        return ResponseEntity.ok(new AlertResponse(AlertMessage.NOTICE_DELETE_SUCCESS.getMessage(), "success"));
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
        log.info("/noticeboard/unread-count: POST, userInfo: {}", userInfo);
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

    @GetMapping("/noticeboard/{noticeId}/comments/count")
    public ResponseEntity<CommonResDto> getCommentCount(@PathVariable Long noticeId) {
        int count = noticeService.getCommentCountByNoticeId(noticeId);
        return ResponseEntity.ok(CommonResDto.success("댓글 수 조회 성공", Map.of("commentCount", count)));
    }
}