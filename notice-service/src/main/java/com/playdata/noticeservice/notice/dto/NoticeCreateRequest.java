package com.playdata.noticeservice.notice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCreateRequest {

    private String title;
    private String content;
    private boolean notice;
    private boolean hasAttachment;

}
