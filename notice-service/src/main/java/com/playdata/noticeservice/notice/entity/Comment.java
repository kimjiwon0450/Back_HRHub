package com.playdata.noticeservice.notice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tbl_comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글 내용
    @Column(nullable = false, length = 1000)
    private String content;

    // 작성자 이름
    @Column(nullable = false)
    private String writerName;

    // 작성자 ID (검증용)
    @Column(nullable = false)
    private Long employeeId;

    // 생성일
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 수정일 (nullable, 최초에는 null)
    private LocalDateTime updatedAt;

    // 게시글과의 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;
}
