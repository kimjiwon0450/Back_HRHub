package com.playdata.hrservice.hr.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrTransferHistoryDto {
    private Long sequenceId;
    private Long departmentId;
    private String positionName;
    private String memo;
}
