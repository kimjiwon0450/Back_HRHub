package com.playdata.approvalservice.approval.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResubmitReqDto {
    // 재상신 시 코멘트는 선택 입력
    private String comment;
}
