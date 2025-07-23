package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.CommunityRead;
import com.playdata.noticeservice.notice.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityReadRepository extends JpaRepository<CommunityRead, Long> {

    // 사용자가 읽은 게시글 ID 리스트
    @Query("SELECT cr.communityId FROM CommunityRead cr WHERE cr.employeeId = :employeeId")
    List<Long> findCommunityIdsByEmployeeId(@Param("employeeId") Long employeeId);

    Optional<CommunityRead> findByCommunityIdAndEmployeeId(Long communityId, Long employeeId);

}
