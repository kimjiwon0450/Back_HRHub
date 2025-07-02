package com.playdata.noticeservice.notice.service;

import com.playdata.noticeservice.notice.dto.NoticeCreateRequest;
import com.playdata.noticeservice.notice.dto.NoticeUpdateRequest;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.NoticeRead;
import com.playdata.noticeservice.notice.repository.NoticeReadRepository;
import com.playdata.noticeservice.notice.repository.NoticeRepository;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;

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

    public void createNotice(NoticeCreateRequest request, Long writerId, Long departmentId) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isNotice(request.isNotice())
                .hasAttachment(request.isHasAttachment())
                .writerId(writerId)
                .departmentId(departmentId)
                .boardStatus(true)
                .build();

        noticeRepository.save(notice);
    }

    @Transactional
    public void updateNotice(Long id, NoticeUpdateRequest request, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getWriterId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setNotice(request.isNotice());
        notice.setHasAttachment(request.isHasAttachment());
        // updatedAt은 @PreUpdate로 자동 설정
    }

    @Transactional
    public void deletePost(Long id,  Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getWriterId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }

        noticeRepository.delete(notice);
    }

    @Transactional
    public void markAsRead(Long noticeId, Long userId) {
        // 이미 읽은 경우 처리 X
        boolean alreadyRead = noticeReadRepository.findByNoticeIdAndUserId(noticeId, userId).isPresent();
        if (alreadyRead) return;

        NoticeRead read = NoticeRead.builder()
                .noticeId(noticeId)
                .userId(userId)
                .readAt(LocalDateTime.now())
                .build();

        noticeReadRepository.save(read);
    }

    public int getUnreadNoticeCount(Long userId) {
        List<Long> readNoticeIds = noticeReadRepository.findNoticeIdsByUserId(userId);
        List<Notice> allNotices = noticeRepository.findByIsNoticeTrueOrderByCreatedAtDesc();

        return (int) allNotices.stream()
                .filter(notice -> !readNoticeIds.contains(notice.getId()))
                .count();
    }

    public List<Notice> getMyPosts(Long userId) {
        return noticeRepository.findByWriterIdOrderByCreatedAtDesc(userId);
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





}





