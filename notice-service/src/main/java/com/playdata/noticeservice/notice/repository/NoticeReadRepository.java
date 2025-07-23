package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.NoticeRead;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

@Repository
public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {

    // 사용자가 읽은 공지글 ID 리스트
    @Query("SELECT nr.noticeId FROM NoticeRead nr WHERE nr.employeeId = :employeeId")
    List<Long> findNoticeIdsByEmployeeId(@Param("employeeId") Long employeeId);

    // [1] 사용자가 읽지 않은 공지글 목록 (부서별, 최신순, 페이징)
    @Query("""
        SELECT n FROM Notice n
        WHERE n.boardStatus = true
          AND (n.departmentId = :departmentId OR n.departmentId = 0)
          AND n.noticeId NOT IN (
              SELECT nr.noticeId FROM NoticeRead nr WHERE nr.employeeId = :employeeId
          )
        ORDER BY n.createdAt DESC
    """)
    List<Notice> findUnreadNoticesByDepartmentAndEmployeeId(
            @Param("departmentId") Long departmentId,
            @Param("employeeId") Long employeeId,
            Pageable pageable
    );

    // [2] 사용자가 읽지 않은 공지글 개수
    @Query("""
        SELECT COUNT(n) FROM Notice n
        WHERE n.boardStatus = true
          AND (n.departmentId = :departmentId1 OR n.departmentId = :departmentId2)
          AND n.noticeId NOT IN (
              SELECT nr.noticeId FROM NoticeRead nr WHERE nr.employeeId = :employeeId
          )
    """)
    int countUnreadNoticesByDepartmentAndEmployeeId(
            @Param("departmentId1") Long departmentId1,
            @Param("departmentId2") Long departmentId2,
            @Param("employeeId") Long employeeId
    );
    Optional<NoticeRead> findByNoticeIdAndEmployeeId(Long noticeId, Long employeeId);

}
