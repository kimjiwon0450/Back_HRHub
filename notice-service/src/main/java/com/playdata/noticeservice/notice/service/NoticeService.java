package com.playdata.noticeservice.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.notice.dto.NoticeCreateRequest;
import com.playdata.noticeservice.notice.dto.NoticeUpdateRequest;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.NoticeAttachment;
import com.playdata.noticeservice.notice.entity.NoticeRead;
import com.playdata.noticeservice.notice.repository.NoticeAttachmentRepository;
import com.playdata.noticeservice.notice.repository.NoticeReadRepository;
import com.playdata.noticeservice.notice.repository.NoticeRepository;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.client.DepartmentClient;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.NoticeResponse;
import java.io.IOException;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileUploadException;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final S3Service s3Service;
    private final HrUserClient hrUserClient;
    private final DepartmentClient departmentClient;
    private final NoticeAttachmentRepository noticeAttachmentRepository;

    // 기존 고정 정렬 버전
    public List<Notice> getAllNotices() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        return noticeRepository.findTopNotices(pageable);
    }

    // 공지글 10개 이후의 공지글만 가져오기
    public List<Notice> getOverflowNoticesAfterTop10() {
        List<Notice> allNotices = noticeRepository.findAllNoticesSorted();
        return allNotices.stream().skip(10).collect(Collectors.toList());
    }


    // 모든 공지글 조회
    public List<Notice> getAllNotices(String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(direction, sortBy));
        return noticeRepository.findTopNotices(pageable);
    }

    // 모든 일반글 조회
    public Page<Notice> getAllPosts(Pageable pageable) {
        return noticeRepository.findAllPosts(pageable);
    }

    // 공지글 10개 이후 + 일반 게시글 전체를 합친 리스트 반환
    public Page<Notice> getMergedPostsAfterTop10(Pageable pageable) {
        List<Notice> overflowNotices = noticeRepository.findAllNoticesSorted()
                .stream()
                .skip(10)
                .collect(Collectors.toList());

        List<Notice> generalPosts = noticeRepository.findAllPosts(PageRequest.of(0, Integer.MAX_VALUE)).getContent(); // 전체 일반글

        List<Notice> merged = new ArrayList<>();
        merged.addAll(overflowNotices);
        merged.addAll(generalPosts);

        // 정렬 (createdAt 기준 내림차순)
        merged.sort(Comparator.comparing(Notice::getCreatedAt).reversed());

        // 수동 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), merged.size());

        List<Notice> pageContent = merged.subList(start, end);
        return new PageImpl<>(pageContent, pageable, merged.size());
    }


    // 필터링된 공지글 조회
    public List<Notice> getFilteredNotices(String keyword, LocalDate from, LocalDate to, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(direction, sortBy));

        if (from == null) {
            from = LocalDate.of(2000, 1, 1); // 매우 과거
        }
        if (to == null) {
            to = LocalDate.now().plusDays(1); // 오늘까지 포함

        }

        return noticeRepository.findFilteredNotices(
                keyword, from, to, pageable);
    }

    // 필터링된 일반글 조회
    public Page<Notice> getFilteredPosts(String keyword, LocalDate from, LocalDate to, Pageable pageable) {
        if (from == null) {
            from = LocalDate.of(2000, 1, 1); // 매우 과거
        }
        if (to == null) {
            to = LocalDate.now().plusDays(1); // 오늘까지 포함

        }

        return noticeRepository.findFilteredPosts(
                keyword, from, to, pageable
        );
    }

    // 내가 쓴 글 조회
    public List<Notice> getMyPosts(Long employeeId) {
        return noticeRepository.findByEmployeeIdAndBoardStatusTrueOrderByCreatedAtDesc(employeeId);
    }

    // 내 부서의 공지글 조회
    public List<Notice> getNoticesByDepartment(Long departmentId, String keyword,
                                                       LocalDate fromDate, LocalDate toDate) {

        Pageable pageable = PageRequest.of(0, 10);

        // 날짜 기본값 처리
        if (fromDate == null) {
            fromDate = LocalDate.of(2000, 1, 1);  // 아주 예전 날짜
        }
        if (toDate == null) {
            toDate = LocalDate.now().plusDays(1);  // 오늘 포함
        }

        return noticeRepository.findMyDepartmentNotices(keyword, fromDate, toDate, departmentId, pageable);
    }

    // 내 부서의 게시글 조회
    public List<Notice> getPostsByDepartment(Long departmentId, String keyword,
                                                     LocalDate fromDate, LocalDate toDate,
                                                     Pageable pageable) {
        // 날짜 기본값 처리
        if (fromDate == null) {
            fromDate = LocalDate.of(2000, 1, 1);  // 아주 예전 날짜
        }
        if (toDate == null) {
            toDate = LocalDate.now().plusDays(1);  // 오늘 포함
        }

        return noticeRepository.findMyDepartmentPosts(keyword, fromDate, toDate, departmentId);
    }

    // 상세 페이지 조회
    public Notice findPostById(Long id) {
        return noticeRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
    }

    // 공지글/게시글 작성
    public void createNotice(NoticeCreateRequest request, Long employeeId, Long departmentId, List<String> attachmentUri) {
        log.info("!!!글 작성!!!");
        log.info(request.getTitle());
        log.info(request.getContent());
        log.info(String.valueOf(request.isNotice()));

        ObjectMapper mapper = new ObjectMapper();
        String attachmentUriJson = "";
        try {
            // 첨부파일 리스트를 JSON 문자열로 변환
            attachmentUriJson = mapper.writeValueAsString(attachmentUri);
        } catch (JsonProcessingException e) {
            log.error("첨부파일 JSON 변환 오류", e);
        }

        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .notice(request.isNotice())
                .attachmentUri(request.getAttachmentUri())
                .employeeId(employeeId)
                .departmentId(departmentId)
                .boardStatus(true)
                .createdAt(LocalDate.now())
                .attachmentUri(attachmentUriJson) // ✅ JSON 배열 형태로 저장
                .build();

        noticeRepository.save(notice);
    }

    // 공지글/게시글 수정
    @Transactional
    public void updateNotice(Long id, NoticeUpdateRequest request, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getEmployeeId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setNotice(request.isNotice());

        ObjectMapper mapper = new ObjectMapper();

        try {
            List<String> attachmentList = Collections.emptyList();
            if (request.getAttachmentUri() != null && !request.getAttachmentUri().isBlank()) {
                attachmentList = mapper.readValue(request.getAttachmentUri(), new TypeReference<List<String>>() {});
            }

            String attachmentUriJson = mapper.writeValueAsString(attachmentList); // ✅ 다시 JSON 문자열로
            notice.setAttachmentUri(attachmentUriJson);
        } catch (JsonProcessingException e) {
            log.error("첨부파일 JSON 직렬화 실패", e);
            throw new RuntimeException("첨부파일 저장 중 오류가 발생했습니다.");
        }
        // updatedAt은 @PreUpdate로 자동 설정
    }

    // 공지글/게시글 삭제
    @Transactional
    public void deletePost(Long id, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getEmployeeId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }

        if (notice.getAttachmentUri() != null) {
            List<String> urls = Arrays.asList(notice.getAttachmentUri().split(","));
            s3Service.deleteFiles(urls);
        }

        notice.setBoardStatus(false); // 삭제 대신 게시글 비활성화

    }

    // 공지글/게시글 읽음 처리
    @Transactional
    public void markAsRead(Long employeeId, Long noticeId) {
        // 이미 읽은 공지인지 확인
        boolean alreadyRead = noticeReadRepository
                .findByNoticeIdAndEmployeeId(noticeId, employeeId)
                .isPresent();
        if (alreadyRead) return;

        // 읽음 기록 저장
        NoticeRead read = NoticeRead.builder()
                .noticeId(noticeId)
                .employeeId(employeeId)
                .readAt(LocalDateTime.now())
                .build();
        noticeReadRepository.save(read);

        // 조회수 증가 및 저장
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다."));
        notice.setViewCount(notice.getViewCount() + 1);
        noticeRepository.save(notice); // 💥 실제로 DB 반영
    }


    // 읽지 않은 공지글 개수 조회
    public int countUnreadNotices(Long employeeId, Long departmentId) {
        return noticeReadRepository.countUnreadNoticesByDepartmentAndEmployeeId(departmentId, employeeId);
    }


    // 읽지 않은 공지글 알림
    public Map<String, List<NoticeResponse>> getUserAlerts(Long employeeId, Long departmentId) {
        Pageable pageable = PageRequest.of(0, 20);

        // 읽지 않은 공지글을 한 번에 조회
        List<Notice> unreadNotices = noticeReadRepository
                .findUnreadNoticesByDepartmentAndEmployeeId(departmentId, employeeId, pageable);

        // 작성자 이름 포함한 DTO로 변환
        List<NoticeResponse> unreadNoticeResponses = unreadNotices.stream()
                .map(notice -> {
                    HrUserResponse writer = hrUserClient.getUserInfo(notice.getEmployeeId());
                    return NoticeResponse.fromEntity(notice, writer);
                })
                .toList();

        return Map.of(
                "unreadNotices", unreadNoticeResponses,
                "otherAlerts", List.of()
        );
    }


}