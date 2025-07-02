package com.playdata.approvalservice.approval.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceReqDto {
    @NotNull(message = "참조자 ID를 입력해주세요.")
    private Long employeeId;
}