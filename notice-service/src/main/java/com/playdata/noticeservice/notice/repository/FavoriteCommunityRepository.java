package com.playdata.noticeservice.notice.repository;

import com.playdata.noticeservice.notice.entity.FavoriteCommunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteCommunityRepository extends JpaRepository<FavoriteCommunity, Long> {
    Optional<FavoriteCommunity> findByUserIdAndCommunityId(Long userId, Long communityId);
    List<FavoriteCommunity> findAllByUserId(Long userId);
    void deleteByUserIdAndCommunityId(Long userId, Long communityId);
}
