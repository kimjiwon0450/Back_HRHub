package com.playdata.hrservice.hr.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrTransferHistoryResDto {
    private Long tranferHistoryId;
    private Long employeeId;
    private List<HrTransferHistoryDto> hrTransferHistories;
}
