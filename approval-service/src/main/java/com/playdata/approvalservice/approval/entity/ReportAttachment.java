package com.playdata.approvalservice.approval.entity;


import com.playdata.approvalservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "report_attachment")
// 첨부파일
public class ReportAttachment extends BaseTimeEntity {

    /**
     * PK, 자동 생성
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_att_id")
    private Long id;

    /**
     * 보고서 결재 id
     */
    @Column(name = "report_approval_id", nullable = false)
    private Long reportApprovalId;

    /**
     * FK → BoardReport.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_approval_id", insertable = false, updatable = false)
    private Reports reports;

    /**
     * 파일명
     */
    @Column(name = "report_att_name")
    private String name;

    /**
     * S3 등 저장소 URL
     */
    @Column(name = "report_att_url", columnDefinition = "TEXT")
    private String url;

    /**
     * 업로드 일시
     */
    @Column(name = "report_att_upload_time")
    private LocalDateTime uploadTime;
}
