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

@Table(name = "favorite_notice", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "notice_id"})
})
public class FavoriteNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
