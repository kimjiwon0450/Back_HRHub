package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalProcessReqDto {
    @NotBlank
    private String action; // APPROVE or REJECT

    private String comment;
}

