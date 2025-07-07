package com.playdata.approvalservice.approval.dto.request;

import com.playdata.approvalservice.approval.entity.ApprovalLine;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResubmitReqDto {
    private String newTitle;
    private ApprovalLine approvalLine;

    private String comment;
}
