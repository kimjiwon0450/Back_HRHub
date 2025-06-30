package com.playdata.approvalservice.approval.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApprovalId implements Serializable {
    private Long reportApprovalId2;
    private Long employeeId;
}
