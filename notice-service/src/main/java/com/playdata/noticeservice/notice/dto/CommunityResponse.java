package com.playdata.noticeservice.notice.dto;

import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.entity.Community;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityResponse {

    private Long communityId;
    private String title;
    private String content;
    private Long employeeId;
    private String name; // 작성자 이름
    private String departmentName;
    private String employStatus;
    private Long departmentId;
    private String attachmentUri;
    private boolean boardStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;

    // 댓글 수
    private int commentCount;


    // 엔티티 -> DTO 변환 정적 메서드
    public static CommunityResponse fromEntity(Community community, HrUserResponse user) {
        return CommunityResponse.builder()
                .communityId(community.getCommunityId())
                .title(community.getTitle())
                .attachmentUri(community.getAttachmentUri())
                .employeeId(community.getEmployeeId())
                .departmentId(community.getDepartmentId())
                .createdAt(community.getCreatedAt())
                .updatedAt(community.getUpdatedAt())
                .viewCount(community.getViewCount())
                .name(user.getName())
                .build();
    }


    public static CommunityResponse fromEntity(Community community, HrUserResponse user, int commentCount) {
        CommunityResponse dto = new CommunityResponse();
        dto.communityId = community.getCommunityId();
        dto.title = community.getTitle();
        dto.content = community.getContent();
        dto.name = user.getName();
        dto.departmentId = community.getDepartmentId();
        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
        dto.employeeId = community.getEmployeeId();
        dto.attachmentUri = community.getAttachmentUri();
        dto.boardStatus = community.isBoardStatus();
        dto.createdAt = community.getCreatedAt();
        dto.updatedAt = community.getUpdatedAt();
        dto.viewCount = community.getViewCount();
        dto.commentCount = commentCount;
        return dto;
    }

    public static CommunityResponse fromEntity(Community community, HrUserResponse user, DepResponse dep) {
        CommunityResponse dto = new CommunityResponse();
        dto.communityId = community.getCommunityId();
        dto.title = community.getTitle();
        dto.content = community.getContent();
        dto.name = user.getName();
        dto.departmentId = community.getDepartmentId();
        dto.departmentName = dep.getName();
        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
        dto.employeeId = community.getEmployeeId();
        dto.attachmentUri = community.getAttachmentUri();
        dto.boardStatus = community.isBoardStatus();
        dto.createdAt = community.getCreatedAt();
        dto.updatedAt = community.getUpdatedAt();
        dto.viewCount = community.getViewCount();
        return dto;
    }

}
