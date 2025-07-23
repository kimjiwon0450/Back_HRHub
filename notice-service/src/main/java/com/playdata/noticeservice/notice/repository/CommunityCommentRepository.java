package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
    List<CommunityComment> findByCommunityIdAndCommentStatusIsTrueOrderByCreatedAtAsc(Long communityId);

    int countByCommunityIdAndCommentStatusTrue(Long communityId);

}
