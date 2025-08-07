package com.playdata.noticeservice.notice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CommentUpdateRequest {
    private String content;

}