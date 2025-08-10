package com.playdata.noticeservice.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.noticeservice.common.client.DepartmentClient;
import com.playdata.noticeservice.common.client.HrUserClient;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.*;
import com.playdata.noticeservice.notice.entity.*;
import com.playdata.noticeservice.notice.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityReadRepository communityReadRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final S3Service s3Service;
    private final HrUserClient hrUserClient;
    private final DepartmentClient departmentClient;
    private final FavoriteCommunityRepository favoriteRepo;


    /**
     * 전체 일반 게시글
     */
    @Transactional(readOnly = true)
    public Page<Community> getAllPosts(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return communityRepository.findAllPosts(pageable);
    }

    /**
     * 필터링된 일반 게시글
     */
    @Transactional(readOnly = true)
    public Page<Community> getFilteredPosts(String keyword, LocalDate fromDate, LocalDate toDate,
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

        return communityRepository.findFilteredPosts(keyword, fromDateTime, toDateTime, pageable);
    }

    /**
     * 내 부서 일반 게시글 (필터링 포함)
     */
    @Transactional(readOnly = true)
    public List<Community> getMyDepartmentPosts(String keyword, LocalDate fromDate, LocalDate toDate, Long departmentId, Pageable pageable) {

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

        return communityRepository.findMyDepartmentPosts(keyword, fromDateTime, toDateTime, departmentId, pageable);
    }

    /**
     * 내가 쓴 일반 게시글
     */
    @Transactional(readOnly = true)
    public List<Community> getMyPosts(String keyword, LocalDate fromDate, LocalDate toDate, Long employeeId, Pageable pageable) {
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

        return communityRepository.findMyPosts(keyword, fromDateTime, toDateTime, employeeId, pageable);
    }


    // 상세 페이지 조회
    public Community findPostById(Long communityI) {
        return communityRepository.findById(communityI).orElseThrow(() -> new RuntimeException("Post not found"));
    }

    // 공지글/게시글 작성
    public void createCommunity(CommunityCreateRequest request, HrUserResponse user, List<String> attachmentUri) {
        log.info("!!!글 작성!!!");
        log.info(request.getTitle());
        log.info(request.getContent());

        ObjectMapper mapper = new ObjectMapper();
        String attachmentUriJson = "";
        try {
            // 첨부파일 리스트를 JSON 문자열로 변환
            attachmentUriJson = mapper.writeValueAsString(attachmentUri);
        } catch (JsonProcessingException e) {
            log.error("첨부파일 JSON 변환 오류", e);
        }

        Long departmentId = user.getDepartmentId();

        Community community = Community.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .attachmentUri(request.getAttachmentUri())
                .employeeId(user.getEmployeeId())
                .departmentId(departmentId)
                .boardStatus(true)
                .createdAt(LocalDateTime.now())
                .attachmentUri(attachmentUriJson) // ✅ JSON 배열 형태로 저장
                .build();

        communityRepository.save(community);
    }

    // 게시글 수정
    @Transactional
    public void updateCommunity(Long communityI, CommunityUpdateRequest request, HrUserResponse user) {
        Community community = communityRepository.findById(communityI)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!community.getEmployeeId().equals(user.getEmployeeId())) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        Long departmentId = user.getDepartmentId();

        community.setTitle(request.getTitle());
        community.setContent(request.getContent());
        community.setDepartmentId(departmentId);

        ObjectMapper mapper = new ObjectMapper();

        try {
            List<String> attachmentList = Collections.emptyList();
            if (request.getAttachmentUri() != null && !request.getAttachmentUri().isBlank()) {
                attachmentList = mapper.readValue(request.getAttachmentUri(), new TypeReference<List<String>>() {});
            }

            String attachmentUriJson = mapper.writeValueAsString(attachmentList); // ✅ 다시 JSON 문자열로
            community.setAttachmentUri(attachmentUriJson);
        } catch (JsonProcessingException e) {
            log.error("첨부파일 JSON 직렬화 실패", e);
            throw new RuntimeException("첨부파일 저장 중 오류가 발생했습니다.");
        }
        // updatedAt은 @PreUpdate로 자동 설정
    }

    // 공지글/게시글 삭제
    @Transactional
    public void deleteCommunity(Long communityI, Long currentUserId) {
        Community community = communityRepository.findById(communityI)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        if (!community.getEmployeeId().equals(currentUserId)) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }

        if (community.getAttachmentUri() != null) {
            List<String> urls = Arrays.asList(community.getAttachmentUri().split(","));
            s3Service.deleteFiles(urls);
        }

        community.setBoardStatus(false); // 삭제 대신 게시글 비활성화

    }

    // 공지글/게시글 읽음 처리
    @Transactional
    public void markAsRead(Long employeeId, Long communityId) {
        // 이미 읽은 공지인지 확인
        boolean alreadyRead = communityReadRepository
                .findByCommunityIdAndEmployeeId(communityId, employeeId)
                .isPresent();
        if (alreadyRead) return;

        // 읽음 기록 저장
        CommunityRead read = CommunityRead.builder()
                .communityId(communityId)
                .employeeId(employeeId)
                .readAt(LocalDateTime.now())
                .build();
        communityReadRepository.save(read);


        communityRepository.incrementViewCount(communityId);
    }

    //////////////////////////댓글 Service///////////////////////////
    // ✅ 댓글 등록
    public void createComment(Long communityId, CommentCreateRequest request, Long employeeId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("해당 게시글이 존재하지 않습니다."));

        CommunityComment comment = CommunityComment.builder()
                .communityId(communityId)
                .content(request.getContent())
                .employeeId(employeeId)
                .writerName(request.getWriterName())
                .authorId(request.getWriterId())
                .commentStatus(true)
                .createdAt(LocalDateTime.now())
                .build();

        // ✅ 대댓글일 경우 부모 설정
        if (request.getParentId() != null) {
            CommunityComment parent = communityCommentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("부모 댓글이 존재하지 않습니다."));
            comment.setParent(parent);
        }

        communityCommentRepository.save(comment);
    }

    // ✅ 댓글 목록 조회
    public List<CommunityCommentResponse> getComments(Long communityId) {
        List<CommunityComment> comments = communityCommentRepository.findByCommunityIdAndCommentStatusIsTrueOrderByCreatedAtAsc(communityId);

//        return comments.stream()
//                .map(comment -> CommunityCommentResponse.builder()
//                        .CommunityComentId(comment.getCommunityCommentId())
//                        .content(comment.getContent())
//                        .writerName(comment.getWriterName())
//                        .createdAt(comment.getCreatedAt())
//                        .build())
//                .toList();
        // ID -> 엔티티 맵
        Map<Long, CommunityCommentResponse> map = new HashMap<>();

        List<CommunityCommentResponse> rootComments = new ArrayList<>();

        for (CommunityComment comment : comments) {
            CommunityCommentResponse response = CommunityCommentResponse.builder()
                    .CommunityComentId(comment.getCommunityCommentId())
                    .content(comment.getContent())
                    .writerName(comment.getWriterName())
                    .createdAt(comment.getCreatedAt())
                    .children(new ArrayList<>())
                    .build();

            map.put(comment.getCommunityCommentId(), response);

            // 부모가 없는 경우 (최상위 댓글)
            if (comment.getParent() == null) {
                rootComments.add(response);
            } else {
                CommunityCommentResponse parentResponse = map.get(comment.getParent().getCommunityCommentId());
                if (parentResponse != null) {
                    parentResponse.getChildren().add(response);
                }
            }
        }

        return rootComments;
    }

    // ✅ 댓글 수정
    public void updateComment(Long communityId, Long commentId, CommentUpdateRequest request, Long employeeId) {
        CommunityComment comment = communityCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        if (!comment.getEmployeeId().equals(employeeId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        communityCommentRepository.save(comment);
    }

    // ✅ 댓글 삭제
    public void deleteComment(Long communityId, Long commentId, Long employeeId) {
        CommunityComment comment = communityCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        if (!comment.getEmployeeId().equals(employeeId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        comment.setCommentStatus(false);
        comment.setUpdatedAt(LocalDateTime.now());
        communityCommentRepository.save(comment);
    }

    // ✅ 댓글 수 조회
    public int getCommentCountByCommunityId(Long communityId) {
        return communityCommentRepository.countByCommunityIdAndCommentStatusTrue(communityId);
    }

    public void toggleFavorite(Long userId, Long communityId) {
        Optional<FavoriteCommunity> existing = favoriteRepo.findByUserIdAndCommunityId(userId, communityId);
        if (existing.isPresent()) {
            favoriteRepo.delete(existing.get());
        } else {
            FavoriteCommunity favorite = new FavoriteCommunity();
            favorite.setUserId(userId);
            favorite.setCommunityId(communityId);
            favoriteRepo.save(favorite);
        }
    }

    public boolean isFavorite(Long userId, Long communityId) {
        return favoriteRepo.findByUserIdAndCommunityId(userId, communityId).isPresent();
    }

    public List<Long> getFavoriteCommunityIds(Long userId) {
        return favoriteRepo.findAllByUserId(userId).stream()
                .map(FavoriteCommunity::getCommunityId)
                .toList();
    }
}