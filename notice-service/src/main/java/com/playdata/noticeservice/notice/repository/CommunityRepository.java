package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    // 일반게시글전체
    @Query("SELECT c FROM Community c WHERE  c.boardStatus = true")
    Page<Community> findAllPosts(Pageable pageable);

    // 일반게시글필터
    @Query("SELECT c FROM Community c WHERE " +
            "c.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR c.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR c.createdAt <= :toDate)")
    Page<Community> findFilteredPosts(@Param("keyword") String keyword,
                                   @Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate,
                                   Pageable pageable);

    // 일반게시글 내부서
    @Query("SELECT c FROM Community c WHERE " +
            "c.boardStatus = true AND " +
            "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:fromDate IS NULL OR c.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR c.createdAt <= :toDate) AND " +
            "c.departmentId = :departmentId")
    List<Community> findMyDepartmentPosts(@Param("keyword") String keyword,
                                       @Param("fromDate") LocalDateTime fromDate,
                                       @Param("toDate") LocalDateTime toDate,
                                       Long departmentId);

    // 일반게시글 내가쓴글
    List<Community> findByEmployeeIdAndBoardStatusTrueOrderByCreatedAtDesc(Long employeeId);


    // 직접 DB 연산으로 조회수 증가
    @Modifying
    @Query("UPDATE Community c SET c.viewCount = c.viewCount + 1 WHERE c.communityId = :id")
    void incrementViewCount(@Param("id") Long id);

    Optional<Community> findByCommunityIdAndBoardStatusTrue(Long communityId);

}










