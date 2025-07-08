package com.playdata.noticeservice.notice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeUpdateRequest {

    private String title;
    private String content;
    private boolean isNotice;
    private boolean hasAttachment;

}
