package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.dto.NoticeResponse;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.Position;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 전체공지글
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.departmentId = 0 AND " +
            "n.position <= :position AND " +
            "n.published = true"
            )
    List<Notice> findAllGeneralNotices(@Param("position") int position,
                                       Pageable pageable);

    // 전체공지글필터
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.departmentId = :departmentId AND " +
            "n.position <= :position AND " +
            "n.published = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    List<Notice> findFilteredGeneralNotices(@Param("position") int position,
                                            @Param("keyword") String keyword,
                                             @Param("fromDate") LocalDateTime fromDate,
                                             @Param("toDate") LocalDateTime toDate,
                                             Long departmentId,
                                             Pageable pageable);

    // 부서공지글(내 부서)
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.departmentId = :departmentId AND " +
            "n.position <= :position AND " +
            "n.published = true")
    Page<Notice> findAllNotices(@Param("position") int position,
                                Long departmentId, Pageable pageable);

    // 부서공지글필터
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.departmentId = :departmentId AND " +
            "n.position <= :position AND " +
            "n.published = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    Page<Notice> findFilteredNotices(@Param("position") int position,
                                     Long departmentId,
                                     @Param("keyword") String keyword,
                                     @Param("fromDate") LocalDateTime fromDate,
                                     @Param("toDate") LocalDateTime toDate,
                                     Pageable pageable);

    // 상세공지페이지
    @Query("SELECT n FROM Notice n " +
            "WHERE n.boardStatus = true " +
            "AND n.published = true " +
            "AND n.noticeId = :noticeId")
    Optional<Notice> findNoticeById(@Param("noticeId") Long noticeId);


    // 부서공지글 내가쓴글
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.published = true AND " +
            "n.employeeId = :employeeId AND" +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    List<Notice> findMyNotices(Long employeeId,
                               @Param("keyword") String keyword,
                               @Param("fromDate") LocalDateTime fromDate,
                               @Param("toDate") LocalDateTime toDate,
                               Pageable pageable);

    // NoticeRepository.java
    List<Notice> findByPublishedFalseAndScheduledAtBefore(LocalDateTime time);

    // 예약한 문서 보기
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.published = false AND " +
            "n.employeeId = :employeeId AND" +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    List<Notice> findMyScheduledNotices(Long employeeId,
                                        @Param("keyword") String keyword,
                                        @Param("fromDate") LocalDateTime fromDate,
                                        @Param("toDate") LocalDateTime toDate,
                                        Pageable pageable);

    // 직접 DB 연산으로 조회수 증가
    @Modifying
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.noticeId = :id")
    void incrementViewCount(@Param("id") Long id);

    // 1) departmentId로 최신 N개의 noticeId를 가져옴 (Pageable로 limit 제어)
    @Query("SELECT n.noticeId FROM Notice n " +
            "WHERE n.departmentId = :departmentId " +
            "ORDER BY n.createdAt DESC")
    List<Long> findTopNoticeIdsByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    // 2) 제외할 ID들을 받아서 결과 조회 (excludedIds가 null이면 제외조건 무시)
    @Query("SELECT n FROM Notice n " +
            "WHERE (n.departmentId = :departmentId OR n.departmentId = 0) " +
            "  AND n.position <= :position " +
            "  AND (:excludedIds IS NULL OR n.noticeId NOT IN :excludedIds) " +
            "ORDER BY n.createdAt DESC")
    Page<Notice> findAllExcludingIds(@Param("position") int position,
                                     @Param("departmentId") Long departmentId,
                                     @Param("excludedIds") List<Long> excludedIds,
                                     Pageable pageable);


}










