package com.playdata.noticeservice.notice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityCreateRequest {

    private String title;
    private String content;
    private long departmentId;
    private String attachmentUri; // JSON 배열 문자열로 S3 파일 URL을 저장
}
