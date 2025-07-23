package com.playdata.noticeservice.notice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_notice_comment")
public class NoticeComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeCommentId;

    private Long noticeId;
    private Long authorId;

    @Column(nullable = false)
    private String content;

    // 작성자 이름
    @Column(nullable = false)
    private String writerName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 작성자 ID (검증용)
    @Column(nullable = false)
    private Long employeeId;

    private boolean commentStatus;

}
