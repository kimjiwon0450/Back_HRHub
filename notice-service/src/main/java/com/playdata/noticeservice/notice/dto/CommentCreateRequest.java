package com.playdata.noticeservice.notice.dto;

import lombok.*;

// 댓글 작성용 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CommentCreateRequest {
    private String content;
    private Long writerId;
    private String writerName;
}
