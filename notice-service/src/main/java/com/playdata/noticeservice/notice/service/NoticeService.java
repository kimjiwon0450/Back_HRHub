package com.playdata.noticeservice.notice.service;

import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.notice.dto.NoticeCreateRequest;
import com.playdata.noticeservice.notice.dto.NoticeUpdateRequest;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.NoticeRead;
import com.playdata.noticeservice.notice.repository.NoticeReadRepository;
import com.playdata.noticeservice.notice.repository.NoticeRepository;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.client.DepartmentClient;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.NoticeResponse;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final S3Service s3Service;
    private final HrUserClient hrUserClient;
    private final DepartmentClient departmentClient;

    public List<Notice> getTopNotices() {
        return noticeRepository.findByIsNoticeTrueOrderByCreatedAtDesc();
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
    public void updateNotice(Long id, NoticeUpdateRequest request, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getEmployeeId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setNotice(request.isNotice());
        notice.setHasAttachment(request.isHasAttachment());
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

        noticeRepository.delete(notice);
    }

    @Transactional
    public void markAsRead( Long userId, Long noticeId) {
        boolean alreadyRead = noticeReadRepository.findByNoticeIdAndUserId(noticeId, userId).isPresent();
        if (alreadyRead) return;

        NoticeRead read = NoticeRead.builder()
                .noticeId(noticeId)
                .userId(userId)
                .readAt(LocalDateTime.now())
                .build();
        noticeReadRepository.save(read);

        // 🔥 조회수 증가
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다."));
        notice.setViewCount(notice.getViewCount() + 1);
    }

    public int getUnreadNoticeCount(Long userId) {
        List<Long> readNoticeIds = noticeReadRepository.findNoticeIdsByUserId(userId);
        List<Notice> allNotices = noticeRepository.findByIsNoticeTrueOrderByCreatedAtDesc();

        return (int) allNotices.stream()
                .filter(notice -> !readNoticeIds.contains(notice.getId()))
                .count();
    }

    public List<NoticeResponse> getMyPosts(Long userId) {
        List<Notice> notices = noticeRepository.findByEmployeeIdOrderByCreatedAtDesc(userId);
        HrUserResponse user = hrUserClient.getUserInfo(userId);

        return notices.stream()
                .map(notice -> NoticeResponse.fromEntity(notice, user.getName()))
                .toList();
    }

    public List<NoticeResponse> getDepartmentPosts(Long userId) {
        HrUserResponse user = hrUserClient.getUserInfo(userId);
        Long departmentId = user.getDepartmentId();
        DepResponse dep = departmentClient.getDepInfo(departmentId);

        List<Notice> notices = noticeRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId);

        return notices.stream()
                .map(notice -> {
                    HrUserResponse writer = hrUserClient.getUserInfo(notice.getEmployeeId());
                    return NoticeResponse.fromEntity(notice, writer.getName(), dep.getName());
                })
                .toList();
    }


    public List<Notice> getTopNoticesByDepartment(Long departmentId) {
        return noticeRepository.findByIsNoticeTrueAndDepartmentIdOrderByCreatedAtDesc(departmentId);
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

    public List<NoticeResponse> getNoticesForListView() {
        List<Notice> notices = noticeRepository.findByIsNoticeTrueOrderByCreatedAtDesc();

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


}