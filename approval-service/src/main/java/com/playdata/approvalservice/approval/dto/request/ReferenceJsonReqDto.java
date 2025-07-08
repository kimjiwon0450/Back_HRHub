// 요청용
package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReferenceJsonReqDto {

    @NotNull(message = "참조자 ID는 필수입니다.")
    private Long employeeId;
}
