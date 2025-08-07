package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.FavoriteNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteNoticeRepository  extends JpaRepository<FavoriteNotice, Long> {
    Optional<FavoriteNotice> findByUserIdAndNoticeId(Long userId, Long noticeId);
    List<FavoriteNotice> findAllByUserId(Long userId);
    void deleteByUserIdAndNoticeId(Long userId, Long noticeId);
}
