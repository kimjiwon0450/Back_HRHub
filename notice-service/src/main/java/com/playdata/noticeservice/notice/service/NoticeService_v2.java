package com.playdata.noticeservice.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.client.DepartmentClient;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.*;
import com.playdata.noticeservice.notice.entity.NoticeComment;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.NoticeRead;
import com.playdata.noticeservice.notice.entity.Position;
import com.playdata.noticeservice.notice.repository.NoticeCommentRepository;
import com.playdata.noticeservice.notice.repository.NoticeReadRepository;
import com.playdata.noticeservice.notice.repository.NoticeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService_v2 {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final S3Service s3Service;
    private final HrUserClient hrUserClient;
    private final DepartmentClient departmentClient;

    private Comparator<Notice> getDynamicComparator(String sortBy, Sort.Direction direction) {
        Comparator<Notice> comparator = switch (sortBy) {
            case "title" ->
                    Comparator.comparing(Notice::getTitle, Comparator.nullsLast(String::compareToIgnoreCase));
            case "createdAt" ->
                    Comparator.comparing(Notice::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" ->
                    Comparator.comparing(Notice::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "viewCount" -> Comparator.comparingInt(Notice::getViewCount);
            default ->
                // 기본은 createdAt
                    Comparator.comparing(Notice::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return direction == Sort.Direction.DESC ? comparator.reversed() : comparator;
    }


    /**
     * 상단 고정 전체 공지글 (전체 부서, 관리자 이상)
     */
    @Transactional(readOnly = true)
    public List<Notice> getTopGeneralNotices(String sortBy, String sortDir, Position position) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        int position_num = position.ordinal();
        List<Notice> allGeneralNotices = noticeRepository.findAllGeneralNotices(position_num, pageable);

        List<Notice> notices = new ArrayList<>(allGeneralNotices);
        notices.sort(getDynamicComparator(sortBy, direction)); // 커스텀 정렬

        return notices;
    }

    /**
     * 필터된 전체 공지글
     */
    @Transactional(readOnly = true)
    public List<Notice> getFilteredGeneralNotices(Position position, Long departmentId,
                                                  String keyword, LocalDate fromDate, LocalDate toDate,
                                                  int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDateTime fromDateTime;
        LocalDateTime toDateTime;

        // 날짜 기본값 처리
        if (fromDate == null) {
            fromDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);  // 아주 예전 날짜
        } else {
            fromDateTime = fromDate.atStartOfDay();
        }

        if (toDate == null) {
            toDateTime = LocalDateTime.now().plusDays(1);  // 오늘 포함
        } else {
            toDateTime = toDate.atTime(23, 59, 59);
        }

        int position_num = position.ordinal();

        return noticeRepository.findFilteredGeneralNotices(position_num, keyword, fromDateTime, toDateTime, departmentId, pageable);
    }

    /**
     * 내 부서 공지글 조회(기본)
     */
    @Transactional(readOnly = true)
    public Page<Notice> getMyDepartmentNotices(Position position, Long departmentId,
                                               int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        int position_num = position.ordinal();

        // 상단 고정용 상위 5개 전체공지글 (createdAt 고정)
        List<Notice> top5GeneralNotices =
                noticeRepository.findAllGeneralNotices(
                        position_num, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        Set<Long> top5GeneralIds = top5GeneralNotices.stream().map(Notice::getNoticeId).collect(Collectors.toSet());

        // 나머지 전체공지글 (정렬 기준 반영)
        List<Notice> sortedGeneralNotices =
                noticeRepository.findAllGeneralNotices(
                        position_num,PageRequest.of(0, 1000, sort)); // 충분히 크게
        List<Notice> overflowGenetalNotices = sortedGeneralNotices.stream()
                .filter(n -> !top5GeneralIds.contains(n.getNoticeId()))
                .toList();

        return noticeRepository.findAllNotices(position_num, departmentId, pageable);
    }

    /**
     * 내 부서 공지글 필터링
     */
    @Transactional(readOnly = true)
    public Page<Notice> getFilteredDepartmentNotices(Position position,
                                                     String keyword, LocalDate fromDate, LocalDate toDate,
                                                     Long departmentId, int page, int size, String sortBy, String sortDir) {
        LocalDateTime fromDateTime;
        LocalDateTime toDateTime;

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));

        // 날짜 기본값 처리
        if (fromDate == null) {
            fromDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);  // 아주 예전 날짜
        } else {
            fromDateTime = fromDate.atStartOfDay();
        }

        if (toDate == null) {
            toDateTime = LocalDateTime.now().plusDays(1);  // 오늘 포함
        } else {
            toDateTime = toDate.atTime(23, 59, 59);
        }

        int position_num =  position.ordinal();
        return noticeRepository.findFilteredNotices(position_num, keyword, fromDateTime, toDateTime, pageable);
    }

    /**
     * 내가 쓴 공지글
     */
    @Transactional(readOnly = true)
    public List<Notice> getMyNotices(Long employeeId) {
        return noticeRepository.findMyNotices(employeeId);
    }

    // 예약한 공지글
    @Transactional(readOnly = true)
    public List<Notice> getMyScheduledNotice(Long employeeId) {
        return noticeRepository.findMyScheduledNotices(employeeId);
    }


    // 상세 페이지 조회
    public Notice findPostById(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(() -> new RuntimeException("Post not found"));
    }

    // 공지글 작성
    public void createNotice(NoticeCreateRequest request, HrUserResponse user, List<String> attachmentUri) {
        log.info("!!!글 작성!!!");
        log.info("request.getTitle() : {}", request.getTitle());
        log.info("request.getContent() : {}", request.getContent());
        log.info("request.getDepartmentId() : {}", request.getDepartmentId());
        log.info("request.getPosition() : {}", request.getPosition());
        log.info("request.getScheduledAt() : {}", request.getScheduledAt());
        log.info("request.isPublished() : {}", request.isPublished());


        ObjectMapper mapper = new ObjectMapper();
        String attachmentUriJson = "";
        try {
            // 첨부파일 리스트를 JSON 문자열로 변환
            attachmentUriJson = mapper.writeValueAsString(attachmentUri);
        } catch (JsonProcessingException e) {
            log.error("첨부파일 JSON 변환 오류", e);
        }

        Long departmentId = user.getDepartmentId();
        if (request.getDepartmentId() == 0L) {
            departmentId = 0L;
        }

        boolean publishedYN = false;
        if (request.getScheduledAt() == null) {
            publishedYN = true;
        }

        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .attachmentUri(request.getAttachmentUri())
                .employeeId(user.getEmployeeId())
                .departmentId(departmentId)
                .boardStatus(true)
                .createdAt(LocalDateTime.now())
                .published(publishedYN)
                .scheduledAt(request.getScheduledAt())
                .attachmentUri(attachmentUriJson) // ✅ JSON 배열 형태로 저장
                .build();
        notice.setPosition(Position.valueOf(request.getPosition()).ordinal());

        noticeRepository.save(notice);
    }

    // 공지글/게시글 수정
    @Transactional
    public void updateNotice(Long noticeId, NoticeUpdateRequest request, HrUserResponse user) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getEmployeeId().equals(user.getEmployeeId())) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        Long departmentId = user.getDepartmentId();
        if (request.getDepartmentId() == 0L) {
            departmentId = 0L;
        }

        boolean publishedYN = false;
        if (request.getScheduledAt() == null) {
            publishedYN = true;
        }

        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setDepartmentId(departmentId);
        notice.setScheduledAt(request.getScheduledAt());
        notice.setPublished(publishedYN);
        notice.setUpdatedAt(LocalDateTime.now());
        notice.setPosition(Position.valueOf(request.getPosition()).ordinal());
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
        noticeRepository.save(notice);
    }

    // 공지글/게시글 삭제
    @Transactional
    public void deletePost(Long noticeId, Long currentUserId) throws JsonProcessingException {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!notice.getEmployeeId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }

        if (notice.getAttachmentUri() != null) {
//            List<String> urls = Arrays.asList(notice.getAttachmentUri().split(","));
            ObjectMapper mapper = new ObjectMapper();
            List<String> urls = mapper.readValue(notice.getAttachmentUri(), new TypeReference<List<String>>() {});
            s3Service.deleteFiles(urls);
        }

        notice.setBoardStatus(false); // 삭제 대신 게시글 비활성화
        noticeRepository.save(notice);

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
//        Notice notice = noticeRepository.findById(noticeId)
//                .orElseThrow(() -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다."));
//        notice.setViewCount(notice.getViewCount() + 1);
//        noticeRepository.save(notice); // 💥 실제로 DB 반영

        noticeRepository.incrementViewCount(noticeId);
    }


    // 읽지 않은 공지글 개수 조회
    public int countUnreadNotices(Position position, Long employeeId, Long departmentId1) {
        Long departmentId2 = 0L;
        int position_num =  position.ordinal();
        return noticeReadRepository.countUnreadNoticesByDepartmentAndEmployeeId(position_num, departmentId1, departmentId2, employeeId);
    }


    // 읽지 않은 공지글 알림
    public Map<String, List<NoticeResponse>> getUserAlerts(Position position, Long employeeId, Long departmentId) {
        Pageable pageable = PageRequest.of(0, 30);
        int position_num =  position.ordinal();
        // 읽지 않은 공지글을 한 번에 조회
        List<Notice> unreadNotices = noticeReadRepository
                .findUnreadNoticesByDepartmentAndEmployeeId(position_num, departmentId, employeeId, pageable);

        // 작성자 이름 포함한 DTO로 변환
        List<NoticeResponse> unreadNoticeResponses = unreadNotices.stream()
                .map(notice -> {
                    HrUserResponse writer = hrUserClient.getUserInfo(notice.getEmployeeId());
                    int commentCount = getCommentCountByNoticeId(notice.getNoticeId());
                    return NoticeResponse.fromEntity(notice, writer, commentCount);
                })
                .toList();

        return Map.of(
                "unreadNotices", unreadNoticeResponses,
                "otherAlerts", List.of()
        );
    }

    //////////////////////////댓글 Service///////////////////////////
    // ✅ 댓글 등록
    public void createComment(Long noticeId, CommentCreateRequest request, Long employeeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("해당 게시글이 존재하지 않습니다."));

        NoticeComment comment = NoticeComment.builder()
                .noticeId(noticeId)
                .content(request.getContent())
                .employeeId(employeeId)
                .writerName(request.getWriterName())
                .authorId(request.getWriterId())
                .commentStatus(true)
                .createdAt(LocalDateTime.now())
                .build();

        noticeCommentRepository.save(comment);
    }

    // ✅ 댓글 목록 조회
    public List<NoticeCommentResponse> getComments(Long noticeId) {
        List<NoticeComment> comments = noticeCommentRepository.findByNoticeIdAndCommentStatusIsTrueOrderByCreatedAtAsc(noticeId);

        return comments.stream()
                .map(comment -> NoticeCommentResponse.builder()
                        .noticeCommentId(comment.getNoticeCommentId())
                        .content(comment.getContent())
                        .writerName(comment.getWriterName())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .toList();
    }

    // ✅ 댓글 수정
    public void updateComment(Long noticeId, Long commentId, CommentUpdateRequest request, Long employeeId) {
        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        if (!comment.getEmployeeId().equals(employeeId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        noticeCommentRepository.save(comment);
    }

    // ✅ 댓글 삭제
    public void deleteComment(Long noticeId, Long commentId, Long employeeId) {
        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        if (!comment.getEmployeeId().equals(employeeId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        comment.setCommentStatus(false);
        comment.setUpdatedAt(LocalDateTime.now());
        noticeCommentRepository.save(comment);
    }

    // ✅ 댓글 수 조회
    public int getCommentCountByNoticeId(Long noticeId) {
        return noticeCommentRepository.countByNoticeIdAndCommentStatusTrue(noticeId);
    }
}