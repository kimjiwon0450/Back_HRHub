package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class ReportReferences {

    @Id
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "emplotee_id")
    private Long emploteeId;

    @Column(name = "report_approval_id")
    private Long reportApprovalId;

}
