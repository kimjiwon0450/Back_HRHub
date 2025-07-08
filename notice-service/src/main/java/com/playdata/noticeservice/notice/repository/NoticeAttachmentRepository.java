package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {
    List<NoticeAttachment> findByNoticeId(Long noticeId);
}
