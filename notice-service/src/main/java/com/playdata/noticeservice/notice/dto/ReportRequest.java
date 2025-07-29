// dto/ReportRequest.java
package com.playdata.noticeservice.notice.dto;

import lombok.Getter;

@Getter
public class ReportRequest {
    private Long reporterId;
    private Long communityId;
    private String reason;
}
