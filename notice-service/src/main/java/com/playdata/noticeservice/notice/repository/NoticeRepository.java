package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.dto.NoticeResponse;
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

    // ✅ 공지글 필터 조회 (isNotice = true)
    @Query("SELECT n FROM Notice n WHERE " +
            "n.isNotice = true AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(n.createdAt >= :fromDate) AND " +
            "(n.createdAt <= :toDate)")
    List<Notice> findFilteredNotices(@Param("keyword") String keyword,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate,
                                     Pageable pageable);


    // ✅ 일반 게시글 필터링 및 페이징
    @Query("SELECT n FROM Notice n WHERE " +
            "n.isNotice = false AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(n.createdAt >= :fromDate) AND " +
            "(n.createdAt <= :toDate)")
    Page<Notice> findFilteredPosts(@Param("keyword") String keyword,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate,
                                   Pageable pageable);

    // 내가 작성한 글
    List<Notice> findByEmployeeIdAndBoardStatusTrueOrderByCreatedAtDesc(Long employeeId);

    // 내 부서의 공지글 (상단 고정용)
    @Query("SELECT n FROM Notice n WHERE " +
            "n.isNotice = true AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(n.createdAt >= :fromDate) AND " +
            "(n.createdAt <= :toDate) AND " +
            "n.departmentId = :departmentId")
    List<Notice> findMyDepartmentNotices(@Param("keyword") String keyword,
                                         @Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate,
                                         Long departmentId, Pageable pageable);

    // 내 부서의 일반글
    @Query("SELECT n FROM Notice n WHERE " +
            "n.isNotice = false AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(n.createdAt >= :fromDate) AND " +
            "(n.createdAt <= :toDate) AND " +
            "n.departmentId = :departmentId")
    List<Notice> findMyDepartmentPosts(@Param("keyword") String keyword,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate,
                                       Long departmentId);

    // 전체 일반글 조회
    @Query("SELECT n FROM Notice n WHERE n.isNotice = false AND n.boardStatus = true ORDER BY n.createdAt DESC")
    Page<Notice> findAllPosts(Pageable pageable);

    // 전체 공지글 조회
    @Query("SELECT n FROM Notice n WHERE n.isNotice = true AND n.boardStatus = true ORDER BY n.createdAt DESC")
    List<Notice> findTopNotices(Pageable pageable);

}