package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import com.playdata.approvalservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "board_report")
public class BoardReport extends BaseTimeEntity {

    /**
     * id
     * PK
     * 자동 생성
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_approval_id")
    private Long id;

    /**
     * 보고서 유형
     */
    @Column(name = "report_approval_type", nullable = false)
    private String type;

    /**
     * 보고서 제목
     */
    @Column(name = "report_title", nullable = false)
    private String title;

    /**
     * 보고서 본문
     */
    @Column(name = "report_content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 상신(제출) 일시
     */
    @Column(name = "report_submitted_at")
    private LocalDateTime submittedAt;

    /**
     * 현재 상태 (DRAFT, IN_PROGRESS 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_approval_status", nullable = false)
    private ReportType status;

    /**
     * 최종 승인 일시
     */
    @Column(name = "report_approved_at")
    private LocalDateTime approvedAt;

    /**
     * 작성자 ID
     */
    @Column(name = "submit_id", nullable = false)
    private Long submitId;

    /**
     * 회수(리콜) 일시
     */
    @Column(name = "report_return_at")
    private LocalDateTime returnAt;

    /**
     * attachments: @OneToMany → ReportAttachment (1:N)
     */
    @Builder.Default
    @OneToMany(mappedBy = "boardReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportAttachment> attachments = new ArrayList<>();

    /**
     * approvalLine: @OneToMany → Approval (1:N), orderSequence 기준 정렬
     */
    @Builder.Default
    @OneToMany(mappedBy = "boardReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderSequence ASC")
    private List<Approval> approvalLine = new ArrayList<>();
}