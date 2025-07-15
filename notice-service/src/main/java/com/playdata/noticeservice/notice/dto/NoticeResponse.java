package com.playdata.noticeservice.notice.dto;

import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.entity.Notice;
import lombok.*;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String content;
    private Long employeeId;
    private String name; // 작성자 이름
    private String departmentName;
    private String employStatus;
    private Long departmentId;
    private boolean notice;
    private String attachmentUri;
    private boolean boardStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;


    // 엔티티 -> DTO 변환 정적 메서드
    public static NoticeResponse fromEntity(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .notice(notice.isNotice())
                .attachmentUri(notice.getAttachmentUri())
                .employeeId(notice.getEmployeeId())
                .departmentId(notice.getDepartmentId())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .viewCount(notice.getViewCount())
                .build();
    }


    public static NoticeResponse fromEntity(Notice notice, HrUserResponse user) {
        NoticeResponse dto = new NoticeResponse();
        dto.id = notice.getId();
        dto.title = notice.getTitle();
        dto.content = notice.getContent();
        dto.name = user.getName();
        dto.departmentId = notice.getDepartmentId();
        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
        dto.employeeId = notice.getEmployeeId();
        dto.notice = notice.isNotice();
        dto.attachmentUri = notice.getAttachmentUri();
        dto.boardStatus = notice.isBoardStatus();
        dto.createdAt = notice.getCreatedAt();
        dto.updatedAt = notice.getUpdatedAt();
        dto.viewCount = notice.getViewCount();
        return dto;
    }

    public static NoticeResponse fromEntity(Notice notice, HrUserResponse user, DepResponse dep) {
        NoticeResponse dto = new NoticeResponse();
        dto.id = notice.getId();
        dto.title = notice.getTitle();
        dto.content = notice.getContent();
        dto.name = user.getName();
        dto.departmentId = notice.getDepartmentId();
        dto.departmentName = dep.getName();
        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
        dto.employeeId = notice.getEmployeeId();
        dto.notice = notice.isNotice();
        dto.attachmentUri = notice.getAttachmentUri();
        dto.boardStatus = notice.isBoardStatus();
        dto.createdAt = notice.getCreatedAt();
        dto.updatedAt = notice.getUpdatedAt();
        dto.viewCount = notice.getViewCount();
        return dto;
    }


}
