package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.NoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {
    Optional<NoticeRead> findByNoticeIdAndEmployeeId(Long noticeId, Long employeeId);

    @Query("SELECT nr.noticeId FROM NoticeRead nr WHERE nr.employeeId = :employeeId")
    List<Long> findNoticeIdsByEmployeeId(@Param("employeeId") Long employeeId);

}