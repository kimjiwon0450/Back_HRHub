package com.playdata.approvalservice.approval.dto.request.template;

import com.playdata.approvalservice.approval.dto.request.ApprovalLineReqDto;
import com.playdata.approvalservice.approval.dto.request.ReferenceJsonReqDto;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ReportFromTemplateReqDto {

    private Long templateId; // 어떤 템플릿을 사용했는지 ID

    private String title; // 사용자가 직접 입력한 문서 제목

    // 템플릿의 빈 칸(ex: 휴가사유, 휴가기간)에 사용자가 입력한 값들
    // ex) {"reason": "개인 사유", "startDate": "2025-08-01", "endDate": "2025-08-05"}
    private Map<String, Object> values;

    // 사용자가 지정한 결재선 정보
    private List<ApprovalLineReqDto> approvalLine;

    // 사용자가 지정한 참조자 정보 (옵션)
    private List<ReferenceJsonReqDto> references;
}