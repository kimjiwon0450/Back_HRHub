package com.playdata.approvalservice.approval.entity;

public enum ReportStatus {
    DRAFT, IN_PROGRESS, APPROVED, REJECTED, RECALLED, SCHEDULED

    /*
        DRAFT: 임시 저장
        IN_PROGRESS: 결재 진행 중 (상신 후)
        APPROVED: 최종 승인
        REJECTED: 반려
        RECALLED: 상신 후 회수
        SCHEDULED: 예약됨
    * */
}
