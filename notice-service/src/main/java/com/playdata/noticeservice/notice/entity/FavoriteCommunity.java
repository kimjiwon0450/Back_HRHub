package com.playdata.noticeservice.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity

@Table(name = "favorite_community", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "community_id"})
})
public class FavoriteCommunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
