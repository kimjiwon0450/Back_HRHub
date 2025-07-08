package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;

@Entity
@Table(name="report_template")
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private long templateId;

    @Column(name = "template", columnDefinition = "JSON")
    private String template;
}
