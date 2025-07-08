package com.playdata.noticeservice.notice.service;

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

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileUploadException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // 모든 공지글 조회
    public List<Notice> getAllNotices() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        return noticeRepository.findTopNotices(pageable);
    }

    // 모든 일반글 조회
    public Page<Notice> getAllPosts(Pageable pageable) {
        return noticeRepository.findAllPosts(pageable);
    }

    // 필터링된 공지글 조회
    public List<Notice> getFilteredNotices(String keyword, LocalDate from, LocalDate to) {
        Pageable pageable = PageRequest.of(0, 10);
        return noticeRepository.findFilteredNotices(
                keyword, from, to, pageable);
    }

    // 필터링된 일반글 조회
    public Page<Notice> getFilteredPosts(String keyword, LocalDate from, LocalDate to, Pageable pageable) {
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
    public void createNotice(NoticeCreateRequest request, Long employeeId, Long departmentId, List<String> fileUrls) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isNotice(request.isNotice())
                .hasAttachment(request.isHasAttachment())
                .employeeId(employeeId)
                .departmentId(departmentId)
                .boardStatus(true)
                .createdAt(LocalDate.now())
                .fileUrls(String.join(",", fileUrls)) // 저장
                .build();

        noticeRepository.save(notice);
    }

    // 공지글/게시글 수정
    @Transactional
    public void updateNotice(Long id, NoticeUpdateRequest request, List<MultipartFile> files, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getEmployeeId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setNotice(request.isNotice());
        notice.setHasAttachment(files != null && !files.isEmpty());
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

        if (notice.isHasAttachment() && notice.getFileUrls() != null) {
            List<String> urls = Arrays.asList(notice.getFileUrls().split(","));
            s3Service.deleteFiles(urls);
        }

        notice.setBoardStatus(false); // 삭제 대신 게시글 비활성화

    }

    // 공지글/게시글 읽음 처리
    @Transactional
    public void markAsRead( Long employeeId, Long noticeId) {
        boolean alreadyRead = noticeReadRepository.findByNoticeIdAndEmployeeId(noticeId, employeeId).isPresent();
        if (alreadyRead) return;

        NoticeRead read = NoticeRead.builder()
                .noticeId(noticeId)
                .employeeId(employeeId)
                .readAt(LocalDateTime.now())
                .build();
        noticeReadRepository.save(read);

        // 🔥 조회수 증가
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다."));
        notice.setViewCount(notice.getViewCount() + 1);
    }

    // 공지글/게시글 안읽은 수 카운트
    public int getUnreadNoticeCount(Long employeeId, String keyword, LocalDate from, LocalDate to, Long departmentId) {
        List<Long> readNoticeIds = noticeReadRepository.findNoticeIdsByEmployeeId(employeeId);
        Pageable pageable = PageRequest.of(0, 100);
        // 날짜 기본값 처리
        if (from == null) {
            from = LocalDate.of(2000, 1, 1);  // 아주 예전 날짜
        }
        if (to == null) {
            to = LocalDate.now().plusDays(1);  // 오늘 포함
        }

        List<Notice> allNotices = noticeRepository.findMyDepartmentNotices(keyword, from, to, departmentId, pageable);

        return (int) allNotices.stream()
                .filter(notice -> !readNoticeIds.contains(notice.getId()))
                .count();
    }


    // 읽지 않은 공지글 알림
    public Map<String, List<NoticeResponse>> getUserAlerts(Long employeeId, Long departmentId) {

        // 2. 사용자가 읽은 공지글 ID 목록
        List<Long> readNoticeIds = noticeReadRepository.findNoticeIdsByEmployeeId(employeeId);

        // 3. 부서별 공지글 중 필터 조건에 맞는 글 10개 조회
        Pageable pageable = PageRequest.of(0, 10);
        List<Notice> allNotices = noticeRepository.findMyDepartmentNotices(null, null, null, departmentId, pageable);

        // 4. 읽지 않은 공지글만 필터링 및 작성자 이름 포함 DTO 변환
        List<NoticeResponse> unreadNoticeResponses = allNotices.stream()
                .filter(notice -> !readNoticeIds.contains(notice.getId()))
                .map(notice -> {
                    HrUserResponse writer = hrUserClient.getUserInfo(notice.getEmployeeId());
                    return NoticeResponse.fromEntity(notice, writer.getName());
                })
                .toList();

        // 7. 기타 알림은 현재는 없음
        List<NoticeResponse> otherAlerts = List.of();

        // 8. Map 형태로 반환
        return Map.of(
                "unreadNotices", unreadNoticeResponses,
                "otherAlerts", otherAlerts
        );
    }

    // 첨부파일 업로드
    @Transactional
    public void uploadNoticeFiles(Long noticeId, List<MultipartFile> files, Long currentUserId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getEmployeeId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 첨부파일을 업로드할 수 있습니다.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                // S3에 업로드 후 URL 반환
                String fileUrl = s3Service.uploadFile(file, "notice/" + noticeId);

                // DB에 저장
                NoticeAttachment attachment = NoticeAttachment.builder()
                        .notice(notice)
                        .originalName(file.getOriginalFilename())
                        .savedName(extractFileNameFromUrl(fileUrl))
                        .uploadPath(fileUrl)
                        .build();

                noticeAttachmentRepository.save(attachment);

            } catch (IOException e) {
                throw new RuntimeException("파일 업로드 실패: " + file.getOriginalFilename());
            }
        }

        notice.setHasAttachment(true);
    }

    // 첨부파일 이름 추춘
    private String extractFileNameFromUrl(String url) {
        if (url == null || !url.contains("/")) return url;
        return url.substring(url.lastIndexOf('/') + 1);
    }



}