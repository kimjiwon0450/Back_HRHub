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

    // 부서공지글 내가쓴글
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.published = true AND " +
            "n.employeeId = :employeeId")
    List<Notice> findMyNotices(Long employeeId);

    // NoticeRepository.java
    List<Notice> findByPublishedFalseAndScheduledAtBefore(LocalDateTime time);

    // 예약한 문서 보기
    @Query("SELECT n FROM Notice n WHERE " +
            "n.boardStatus = true AND " +
            "n.published = false AND " +
            "n.employeeId = :employeeId")
    List<Notice> findMyScheduledNotices(Long employeeId);

    // 직접 DB 연산으로 조회수 증가
    @Modifying
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.noticeId = :id")
    void incrementViewCount(@Param("id") Long id);

}










