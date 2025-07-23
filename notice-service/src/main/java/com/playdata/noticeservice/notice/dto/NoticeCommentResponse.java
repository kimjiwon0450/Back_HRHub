package com.playdata.noticeservice.notice.dto;

import lombok.*;

import java.time.LocalDateTime;

// 댓글 응답용 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class NoticeCommentResponse {
    private Long noticeCommentId;
    private String content;
    private String writerName;
    private LocalDateTime createdAt;
}
