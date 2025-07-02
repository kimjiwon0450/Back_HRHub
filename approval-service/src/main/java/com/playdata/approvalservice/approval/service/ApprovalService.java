package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.dto.request.*;
import com.playdata.approvalservice.approval.dto.response.*;
import com.playdata.approvalservice.approval.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApprovalService {

    private final BoardReportRepository boardReportRepository;
    private final ApprovalRepository approvalRepository;
    private final ReportAttachmentRepository attachmentRepository;

    @Transactional
    public ReportCreateResDto createReport(ReportCreateReqDto req, Long userId) {

        return null;
    }

    @Transactional
    public ReportUpdateResDto updateReport(Long reportId, ReportUpdateReqDto req, Long userId) {

        return null;
    }

    public ReportListResDto getReports(String role, String status, int page, int size,
                                       String keyword, String from, String to, Long userId) {

        return null;
    }

    public ReportDetailResDto getReportDetail(Long reportId, Long userId) {

        return null;
    }

    @Transactional
    public ApprovalProcessResDto processApproval(Long reportId, Long userId, ApprovalProcessReqDto req) {

        return null;
    }

    public ApprovalHistoryResDto getApprovalHistory(Long reportId) {

        return null;
    }

    @Transactional
    public ReportSubmitResDto submitReport(Long reportId, Long userId, SubmitReportReqDto req) {

        return null;
    }

    @Transactional
    public void deleteReport(Long reportId, Long userId) {

    }

    @Transactional
    public ReportRecallResDto recallReport(Long reportId, Long userId) {

        return null;
    }

    public ReportRemindResDto reportRemind(Long reportId, Long userId) {
        return null;
    }

    public ResubmitResDto resubmitReport(Long reportId, Long userId, ResubmitReqDto req) {
    }

    public ReportReferencesResDto deleteReferences(Long reportId, Long userId, Long employeeId) {
    }

    public ReferenceResDto addReference(Long reportId, Long userId, @Valid ReferenceReqDto req) {
    }

    public AttachmentResDto uploadAttachment(Long reportId, Long userId, @Valid AttachmentReqDto req) {
    }

    public CommentResDto createComment(Long reportId, Long userId, @Valid CommentReqDto req) {
    }
}

