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
public class CommunityCommentResponse {
    private Long CommunityComentId;
    private String content;
    private String writerName;
    private LocalDateTime createdAt;
}
