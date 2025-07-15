package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.dto.NoticeResponse;
import com.playdata.noticeservice.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 전체 부서 공지글 필터 조회
    @Query("SELECT n FROM Notice n WHERE " +
            "n.notice = true AND " +
            "n.boardStatus = true AND " +
            "n.departmentId = :departmentId AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    List<Notice> findFilteredGeneralNotices(@Param("keyword") String keyword,
                                     @Param("fromDate") LocalDateTime fromDate,
                                     @Param("toDate") LocalDateTime toDate,
                                     Long departmentId,
                                     Pageable pageable);

    // ✅ 공지글 필터 조회 (isNotice = true)
    @Query("SELECT n FROM Notice n WHERE " +
            "n.notice = true AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    List<Notice> findFilteredNotices(@Param("keyword") String keyword,
                                     @Param("fromDate") LocalDateTime fromDate,
                                     @Param("toDate") LocalDateTime toDate,
                                     Pageable pageable);


    // ✅ 게시글 필터 조회
    @Query("SELECT n FROM Notice n WHERE " +
            "n.notice = false AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate)")
    Page<Notice> findFilteredPosts(@Param("keyword") String keyword,
                                   @Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate,
                                   Pageable pageable);

    // 내가 작성한 글
    List<Notice> findByEmployeeIdAndBoardStatusTrueOrderByCreatedAtDesc(Long employeeId);

    // 내 부서의 공지글 (상단 고정용)
    @Query("SELECT n FROM Notice n WHERE " +
            "n.notice = true AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate) AND " +
            "n.departmentId = :departmentId")
    List<Notice> findMyDepartmentNotices(@Param("keyword") String keyword,
                                         @Param("fromDate") LocalDateTime fromDate,
                                         @Param("toDate") LocalDateTime toDate,
                                         Long departmentId, Pageable pageable);

    // 내 부서의 일반글
    @Query("SELECT n FROM Notice n WHERE " +
            "n.notice = false AND " +
            "n.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR n.createdAt <= :toDate) AND " +
            "n.departmentId = :departmentId")
    List<Notice> findMyDepartmentPosts(@Param("keyword") String keyword,
                                       @Param("fromDate") LocalDateTime fromDate,
                                       @Param("toDate") LocalDateTime toDate,
                                       Long departmentId);

    // 전체 일반글 조회
    @Query("SELECT n FROM Notice n WHERE n.notice = false AND n.boardStatus = true")
    Page<Notice> findAllPosts(Pageable pageable);

    // 전체 공지글 조회
    @Query("SELECT n FROM Notice n WHERE n.notice = true AND n.boardStatus = true")
    List<Notice> findTopNotices(Pageable pageable);

    // 공지글 전체 (정렬 포함)
    @Query("SELECT n FROM Notice n WHERE n.notice = true AND n.boardStatus = true ORDER BY n.createdAt DESC")
    List<Notice> findOverflowNotices(Pageable pageable);

    // departmentId가 0인 부서전체 공지 조회
    List<Notice> findByDepartmentIdAndBoardStatusTrueOrderByCreatedAtDesc(Long departmentId);

    // 직접 DB 연산으로 조회수 증가
    @Modifying
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") Long id);

}