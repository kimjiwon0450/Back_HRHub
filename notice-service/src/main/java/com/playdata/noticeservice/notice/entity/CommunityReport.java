package com.playdata.noticeservice.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @ToString
@Builder @AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tbl_community_report")
public class CommunityReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long communityReportId;

    private Long communityId;

    private Long reporterId;   // 신고한 사용자 ID or 이름
    private String reason;     // 신고 사유

    private boolean resolved = false;  // 처리 여부
    private LocalDateTime createdAt = LocalDateTime.now();
}
