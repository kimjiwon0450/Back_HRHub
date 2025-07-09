package com.playdata.noticeservice.notice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCreateRequest {

    private String title;
    private String content;
    private boolean notice;
    private String attachmentUri; // JSON 배열 문자열로 S3 파일 URL을 저장


}
