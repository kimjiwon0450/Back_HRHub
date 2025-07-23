package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.NoticeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeCommentRepository extends JpaRepository<NoticeComment , Long> {
    List<NoticeComment> findByNoticeIdAndCommentStatusIsTrueOrderByCreatedAtAsc(Long noticeId);

    int countByNoticeIdAndCommentStatusTrue(Long noticeId);

}
