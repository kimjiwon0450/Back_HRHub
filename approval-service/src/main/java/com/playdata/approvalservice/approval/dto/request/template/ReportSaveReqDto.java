package com.playdata.approvalservice.approval.dto.request.template;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ReportSaveReqDto {
    private Long templateId;
    private Map<String, Object> formData; // 사용자가 입력하고 수정한 최종 데이터
    private List<Long> approvalLine; // 결재자 ID 목록
    // 첨부파일 등은 @RequestPart로 별도 처리
}