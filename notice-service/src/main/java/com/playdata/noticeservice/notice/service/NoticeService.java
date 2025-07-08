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


    public List<Notice> getTopNotices(String keyword, LocalDate from, LocalDate to, Long departmentId) {
        Pageable pageable = PageRequest.of(0, 10);
        return noticeRepository.findFilteredNotices(keyword, from, to, departmentId, pageable);
    }

    public Page<Notice> getFilteredPosts(String keyword, LocalDate from, LocalDate to, Long departmentId, Pageable pageable) {
        return noticeRepository.findFilteredPosts(
                keyword, from, to, departmentId, pageable
        );
    }


    public List<Notice> findAllPosts() {
        return noticeRepository.findAll();
    }


    public Notice findPostById(Long id) {
        return noticeRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public void createNotice(NoticeCreateRequest request, Long employeeId, Long departmentId, List<String> fileUrls) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isNotice(request.isNotice())
                .hasAttachment(request.isHasAttachment())
                .employeeId(employeeId)
                .departmentId(departmentId)
                .boardStatus(true)
                .fileUrls(String.join(",", fileUrls)) // 저장
                .build();

        noticeRepository.save(notice);
    }


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

    public int getUnreadNoticeCount(Long employeeId, String keyword, LocalDate from, LocalDate to, Long departmentId) {
        List<Long> readNoticeIds = noticeReadRepository.findNoticeIdsByEmployeeId(employeeId);
        Pageable pageable = PageRequest.of(0, 100);
        List<Notice> allNotices = noticeRepository.findFilteredNotices(keyword, from, to, departmentId, pageable);

        return (int) allNotices.stream()
                .filter(notice -> !readNoticeIds.contains(notice.getId()))
                .count();
    }

    public List<NoticeResponse> getMyPosts(Long employeeId) {
        List<Notice> notices = noticeRepository.findByEmployeeIdAndBoardStatusTrueOrderByCreatedAtDesc(employeeId);
        HrUserResponse user = hrUserClient.getUserInfo(employeeId);

        return notices.stream()
                .map(notice -> NoticeResponse.fromEntity(notice, user.getName()))
                .toList();
    }

    public List<NoticeResponse> getDepartmentPosts(Long employeeId) {
        HrUserResponse user = hrUserClient.getUserInfo(employeeId);
        Long departmentId = user.getDepartmentId();
        DepResponse dep = departmentClient.getDepInfo(departmentId);
        List<Notice> notices = noticeRepository.findByDepartmentIdAndBoardStatusTrueOrderByCreatedAtDesc(departmentId);

        return notices.stream()
                .map(notice -> {
                    HrUserResponse writer = hrUserClient.getUserInfo(notice.getEmployeeId());
                    return NoticeResponse.fromEntity(notice, writer.getName(), dep.getName());
                })
                .toList();
    }


    public List<Notice> getTopNoticesByDepartment(Long departmentId) {
        Pageable pageable = PageRequest.of(0, 10);
        return noticeRepository.findByIsNoticeTrueAndBoardStatusTrueAndDepartmentIdOrderByCreatedAtDesc(departmentId, pageable);
    }

    public Page<Notice> getPostsByDepartment(Long departmentId, String keyword,
                                             LocalDate fromDate, LocalDate toDate,
                                             Pageable pageable) {
        // 날짜 기본값 처리
        if (fromDate == null) {
            fromDate = LocalDate.of(2000, 1, 1);  // 아주 예전 날짜
        }
        if (toDate == null) {
            toDate = LocalDate.now().plusDays(1);  // 오늘 포함
        }

        return noticeRepository.findFilteredPosts(keyword, fromDate, toDate, departmentId, pageable);
    }

    public List<NoticeResponse> getNoticesForListView(String keyword, LocalDate from, LocalDate to, Long departmentId) {
        Pageable pageable = PageRequest.of(0, 10);
        List<Notice> notices = noticeRepository.findFilteredNotices(keyword, from, to, departmentId, pageable);

        return notices.stream().map(notice -> {
            HrUserResponse user = hrUserClient.getUserInfo(notice.getEmployeeId());

            return NoticeResponse.builder()
                    .id(notice.getId())
                    .title(notice.getTitle())
                    .content(notice.getContent())
                    .name(user.getName()) // ✅ 이름 세팅
                    .isNotice(notice.isNotice())
                    .hasAttachment(notice.isHasAttachment())
                    .createdAt(notice.getCreatedAt())
                    .viewCount(notice.getViewCount())
                    .build();
        }).toList();
    }


    public Map<String, List<NoticeResponse>> getUserAlerts(Long employeeId) {
        // 1. 사용자의 부서 ID 가져오기
        HrUserResponse userInfo = hrUserClient.getUserInfo(employeeId);
        Long departmentId = userInfo.getDepartmentId();

        // 3. 사용자가 읽은 공지글 ID 목록
        List<Long> readNoticeIds = noticeReadRepository.findNoticeIdsByEmployeeId(employeeId);

        // 4. 부서별 공지글 중 필터 조건에 맞는 글 10개 조회
        Pageable pageable = PageRequest.of(0, 10);
        List<Notice> allNotices = noticeRepository.findFilteredNotices(null, null, null, departmentId, pageable);

        // 5. 읽지 않은 공지글만 필터링
        List<Notice> unreadNotices = allNotices.stream()
                .filter(notice -> !readNoticeIds.contains(notice.getId()))
                .toList();

        // 6. 작성자 이름 주입 후 DTO로 변환
        List<NoticeResponse> unreadNoticeResponses = unreadNotices.stream()
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

    private String extractFileNameFromUrl(String url) {
        if (url == null || !url.contains("/")) return url;
        return url.substring(url.lastIndexOf('/') + 1);
    }



}