package com.playdata.noticeservice.notice.dto;

import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.entity.Community;
import com.playdata.noticeservice.notice.entity.CommunityReport;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityReportResponse {

    private Long communityId;
    private String title;
    private String content;
    private Long employeeId;
    private String name; // 작성자 이름
    private boolean boardStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long communityReportId;
    private Long reporterId;   // 신고한 사용자
    private String reporterName; // 신고한 사용자  이름
    private String reason;     // 신고 사유

    private boolean resolved = false;  // 처리 여부
    private LocalDateTime reportCreatedAt = LocalDateTime.now();




    // 엔티티 -> DTO 변환 정적 메서드
    public static CommunityReportResponse fromEntity(CommunityReport communityReport, HrUserResponse user, CommunityResponse community) {
        return CommunityReportResponse.builder()
                .communityId(communityReport.getCommunityId())
                .reporterId(communityReport.getReporterId())
                .reporterName(user.getName())
                .communityReportId(communityReport.getCommunityReportId())
                .reason(communityReport.getReason())
                .reportCreatedAt(communityReport.getCreatedAt())
                .resolved(communityReport.isResolved())
                .title(community.getTitle())
                .content(community.getContent())
                .employeeId(community.getEmployeeId())
                .name(community.getName())
                .build();
    }


//    public static CommunityReportResponse fromEntity(Community community, HrUserResponse user, int commentCount) {
//        CommunityReportResponse dto = new CommunityReportResponse();
//        dto.communityId = community.getCommunityId();
//        dto.title = community.getTitle();
//        dto.content = community.getContent();
//        dto.name = user.getName();
//        dto.departmentId = community.getDepartmentId();
//        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
//        dto.employeeId = community.getEmployeeId();
//        dto.attachmentUri = community.getAttachmentUri();
//        dto.boardStatus = community.isBoardStatus();
//        dto.createdAt = community.getCreatedAt();
//        dto.updatedAt = community.getUpdatedAt();
//        dto.viewCount = community.getViewCount();
//        dto.commentCount = commentCount;
//        return dto;
//    }
//
//    public static CommunityReportResponse fromEntity(Community community, HrUserResponse user, DepResponse dep) {
//        CommunityReportResponse dto = new CommunityReportResponse();
//        dto.communityId = community.getCommunityId();
//        dto.title = community.getTitle();
//        dto.content = community.getContent();
//        dto.name = user.getName();
//        dto.departmentId = community.getDepartmentId();
//        dto.departmentName = dep.getName();
//        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
//        dto.employeeId = community.getEmployeeId();
//        dto.attachmentUri = community.getAttachmentUri();
//        dto.boardStatus = community.isBoardStatus();
//        dto.createdAt = community.getCreatedAt();
//        dto.updatedAt = community.getUpdatedAt();
//        dto.viewCount = community.getViewCount();
//        return dto;
//    }

}
