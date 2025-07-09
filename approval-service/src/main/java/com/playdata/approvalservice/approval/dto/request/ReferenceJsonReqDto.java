// 요청용
package com.playdata.approvalservice.approval.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReferenceJsonReqDto {

    private Long employeeId;

}
