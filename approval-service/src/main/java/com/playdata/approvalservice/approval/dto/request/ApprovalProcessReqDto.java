package com.playdata.approvalservice.approval.dto.request;

import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalProcessReqDto {
    @NotNull
    private ApprovalStatus approvalStatus; // APPROVE or REJECT

    private String comment;
}

