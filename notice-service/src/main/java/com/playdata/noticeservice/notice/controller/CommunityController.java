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
import com.playdata.noticeservice.notice.entity.Community;
import com.playdata.noticeservice.notice.service.CommunityService;
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
@RequestMapping("/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityController {

    private final Environment env;
    private final CommunityService communityService;
    private final HrUserClient hrUserClient;
    private final DepartmentClient departmentClient;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    @GetMapping()
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

        Page<Community> posts;
        if (hasFilter) {
            posts = communityService.getFilteredPosts(keyword, fromDate, toDate, page, pageSize, sortBy, sortDir);
        } else {
            posts = communityService.getAllPosts(page, pageSize, sortBy, sortDir);
        }

        Set<Long> employeeIds = posts.stream()
                .map(Community::getEmployeeId)
                .collect(Collectors.toSet());

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(employeeIds, token)
                .stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts.stream()
                .map(c -> {
                    HrUserResponse writer = userMap.get(c.getEmployeeId());
                    int commentCount = communityService.getCommentCountByCommunityId(c.getCommunityId());
                    return CommunityResponse.fromEntity(c, writer, commentCount);
                }).toList());
        response.put("totalPages", posts.getTotalPages());
        response.put("currentPage", posts.getNumber());

        return ResponseEntity.ok(response);
    }


    // 내가 쓴 글 조회(일반글)
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyPosts(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Community> community = communityService.getMyPosts(keyword, fromDate, toDate, userInfo.getEmployeeId(),pageable);

        String token = request.getHeader("Authorization");

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(
                community.stream().map(Community::getEmployeeId).collect(Collectors.toSet())
                , token).stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();

        response.put("myposts", community.stream()
                .map(c -> {
                    HrUserResponse user = userMap.get(c.getEmployeeId());
                    int commentCount = communityService.getCommentCountByCommunityId(c.getCommunityId()); // ✅ 댓글 수
                    return CommunityResponse.fromEntity(c, user, commentCount); // ✅ 댓글 수 포함
                }).toList());
        return ResponseEntity.ok(response);
    }


    // 일반게시글 나의 부서글 조회()
    @GetMapping("/mydepartment")
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
        List<Community> posts = communityService.getMyDepartmentPosts(keyword, fromDate, toDate, userInfo.getDepartmentId(),pageable);

        Map<Long, HrUserResponse> userMap = hrUserClient.getUserInfoBulk(
                posts.stream().map(Community::getEmployeeId).collect(Collectors.toSet())
        , token).stream().collect(Collectors.toMap(HrUserResponse::getEmployeeId, Function.identity()));

        Map<String, Object> response = new HashMap<>();

        response.put("mydepposts", posts.stream()
                .map(c -> {
                    HrUserResponse user = userMap.get(c.getEmployeeId());
                    int commentCount = communityService.getCommentCountByCommunityId(c.getCommunityId()); // ✅ 댓글 수
                    return CommunityResponse.fromEntity(c, user, commentCount); // ✅ 댓글 수 포함
                }).toList());
        return ResponseEntity.ok(response);
    }


    // 글 상세 화면 조회
    @GetMapping("/{communityId:\\d+}")
    public ResponseEntity<CommunityResponse> getCommunityPost(@PathVariable Long communityId) {
        Community community = communityService.findPostById(communityId);
        HrUserResponse user = hrUserClient.getUserInfo(community.getEmployeeId());
        DepResponse dep = departmentClient.getDepInfo(community.getDepartmentId());
        return ResponseEntity.ok(CommunityResponse.fromEntity(community, user, dep));
    }

    // 글 작성 페이지
    @PostMapping("/write")
    public ResponseEntity<AlertResponse> createCommunity(
            @RequestBody @Valid CommunityCreateRequest request,
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
        communityService.createCommunity(request, user, attachmentUri);
        return ResponseEntity.ok(new AlertResponse(AlertMessage.NOTICE_CREATE_SUCCESS.getMessage(), "success"));
    }

    @GetMapping("/upload-url")
    public ResponseEntity<String> generateUploadCommunityUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        String url = s3Service.generatePresignedUrlForPut(fileName, contentType);
        return ResponseEntity.ok(url);
    }


    @GetMapping("/download-url")
    public ResponseEntity<String> generateDownloadCommunityUrl(@RequestParam String fileName) {
        String url = s3Service.generatePresignedUrlForGet(fileName, "application/octet-stream");
        return ResponseEntity.ok(url);
    }


    // 글 수정 페이지
    @PutMapping(value = "/edit/{communityId:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertResponse> updateCommunity(
            @PathVariable Long communityId,
            @RequestBody @Valid CommunityUpdateRequest request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {

        Long employeeId = userInfo.getEmployeeId();
        HrUserResponse user = hrUserClient.getUserInfo(employeeId);
        // 파일이 없기 때문에 null 전달 또는 별도 처리
        communityService.updateCommunity(communityId, request, user);
        return ResponseEntity.ok(new AlertResponse(AlertMessage.NOTICE_UPDATE_SUCCESS.getMessage(), "success"));
    }


    // 글 삭제
    @DeleteMapping("/delete/{communityId:\\d+}")
    public ResponseEntity<AlertResponse> deleteCommunity(@PathVariable Long communityId,
                                                    @AuthenticationPrincipal TokenUserInfo userInfo) {
        communityService.deleteCommunity(communityId, userInfo.getEmployeeId());
//        return ResponseEntity.noContent().build();
        return ResponseEntity.ok(new AlertResponse(AlertMessage.NOTICE_DELETE_SUCCESS.getMessage(), "success"));
    }

    // ✅ 공지글 읽음 처리
    @PostMapping("/{communityId:\\d+}/read")
    public ResponseEntity<Void> markAsReadCommunity(@AuthenticationPrincipal TokenUserInfo userInfo,
                                           @PathVariable Long communityId) {
        log.info("/community/{}/read: POST", communityId);
        log.info("userInfo: {}", userInfo);
        communityService.markAsRead(userInfo.getEmployeeId(), communityId);
        return ResponseEntity.ok().build();
    }


    ///////////////////////////댓글 Controller//////////////////////////////

    // ✅ 댓글 작성
    @PostMapping("/{communityId:\\d+}/comments")
    public ResponseEntity<Void> createCoummunityComment(@PathVariable Long communityId,
                                              @RequestBody @Valid CommentCreateRequest request,
                                              @AuthenticationPrincipal TokenUserInfo userInfo) {
        communityService.createComment(communityId, request, userInfo.getEmployeeId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ✅ 댓글 목록 조회
    @GetMapping("/{communityId:\\d+}/comments")
    public ResponseEntity<List<CommunityCommentResponse>> getCommunityComments(@PathVariable Long communityId) {
        List<CommunityCommentResponse> comments = communityService.getComments(communityId);
        return ResponseEntity.ok(comments);
    }

    // ✅ 댓글 수정
    @PutMapping("/{communityId:\\d+}/comments/{commentId}")
    public ResponseEntity<Void> updateCommunityComment(@PathVariable Long communityId,
                                              @PathVariable Long commentId,
                                              @RequestBody @Valid CommentUpdateRequest request,
                                              @AuthenticationPrincipal TokenUserInfo userInfo) {
        communityService.updateComment(communityId, commentId, request, userInfo.getEmployeeId());
        return ResponseEntity.ok().build();
    }

    // ✅ 댓글 삭제
    @DeleteMapping("/{communityId:\\d+}/comments/{commentId}")
    public ResponseEntity<Void> deleteCommunityComment(@PathVariable Long communityId,
                                              @PathVariable Long commentId,
                                              @AuthenticationPrincipal TokenUserInfo userInfo) {
        communityService.deleteComment(communityId, commentId, userInfo.getEmployeeId());
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{communityId:\\d+}/comments/count")
    public ResponseEntity<CommonResDto> getCommunityCommentCount(@PathVariable Long communityId) {
        int count = communityService.getCommentCountByCommunityId(communityId);
        return ResponseEntity.ok(CommonResDto.success("댓글 수 조회 성공", Map.of("commentCount", count)));
    }
}