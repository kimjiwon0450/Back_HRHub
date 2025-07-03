package com.playdata.approvalservice.approval.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportListResDto {
    private List<ReportSimpleDto> reports;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;

    @Getter @Setter @ToString
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReportSimpleDto {
        private Long id;
        private String title;
        private String name;
        private String createdAt;
        private String status;
        private String currentApprover;
    }
}