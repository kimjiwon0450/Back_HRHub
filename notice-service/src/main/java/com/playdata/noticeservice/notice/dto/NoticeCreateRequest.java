package com.playdata.noticeservice.notice.dto;

import com.playdata.noticeservice.notice.entity.Position;
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
    private long departmentId;
    private String attachmentUri; // JSON 배열 문자열로 S3 파일 URL을 저장
    private String position;
}
