package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalLineReqDto {
    @NotNull
    private Long employeeId;

    @NotNull
    private int order;
}

