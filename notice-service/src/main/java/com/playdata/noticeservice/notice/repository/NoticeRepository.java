package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // ✅ 공지글 전체 조회 (isNotice = true)
    @Query("SELECT n FROM Notice n WHERE " +
            "n.isNotice = true AND " +
            "n.boardStatus = true AND " +
            "LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate) AND " +
            "(:departmentId IS NULL OR n.departmentId = :departmentId)")
    List<Notice> findFilteredNotices(@Param("keyword") String keyword,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate,
                                     @Param("departmentId") Long departmentId,
                                     Pageable pageable);


    // ✅ 일반 게시글 필터링 및 페이징
    @Query("SELECT n FROM Notice n WHERE " +
            "n.isNotice = false AND " +
            "n.boardStatus = true AND " +
            "LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate) AND " +
            "(:departmentId IS NULL OR n.departmentId = :departmentId)")
    Page<Notice> findFilteredPosts(@Param("keyword") String keyword,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate,
                                   @Param("departmentId") Long departmentId,
                                   Pageable pageable);

    // 내가 작성한 글
    List<Notice> findByEmployeeIdAndBoardStatusTrueOrderByCreatedAtDesc(Long employeeId);

    // 부서별 공지글 (상단 고정용)
    List<Notice> findByIsNoticeTrueAndBoardStatusTrueAndDepartmentIdOrderByCreatedAtDesc(Long departmentId, Pageable pageable);

    // 부서별 일반글
    List<Notice> findByDepartmentIdAndBoardStatusTrueOrderByCreatedAtDesc(Long departmentId);
}
