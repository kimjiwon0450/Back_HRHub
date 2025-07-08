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
    @NotNull(message = "결재 상태는 필수입니다.")
    private ApprovalStatus approvalStatus; // APPROVE or REJECT

    private String comment;
}

