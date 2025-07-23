package com.playdata.noticeservice.notice.dto;

import com.playdata.noticeservice.common.dto.DepResponse;
import com.playdata.noticeservice.common.dto.HrUserResponse;
import com.playdata.noticeservice.notice.entity.Notice;
import com.playdata.noticeservice.notice.entity.Position;
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

    private Long noticeId;
    private String title;
    private String content;
    private Long employeeId;
    private String name; // 작성자 이름
    private String departmentName;
    private String employStatus;
    private Long departmentId;
    private boolean general;
    private String attachmentUri;
    private boolean boardStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;
    private String position;

    // 댓글 수
    private int commentCount;


    // 엔티티 -> DTO 변환 정적 메서드
    public static NoticeResponse fromEntity(Notice notice) {
        return NoticeResponse.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .attachmentUri(notice.getAttachmentUri())
                .employeeId(notice.getEmployeeId())
                .departmentId(notice.getDepartmentId())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .viewCount(notice.getViewCount())
                .build();
    }


    public static NoticeResponse fromEntity(Notice notice, HrUserResponse user, int commentCount) {
        NoticeResponse dto = new NoticeResponse();
        dto.noticeId = notice.getNoticeId();
        dto.title = notice.getTitle();
        dto.content = notice.getContent();
        dto.name = user.getName();
        dto.departmentId = notice.getDepartmentId();
        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
        dto.employeeId = notice.getEmployeeId();
        dto.attachmentUri = notice.getAttachmentUri();
        dto.boardStatus = notice.isBoardStatus();
        dto.createdAt = notice.getCreatedAt();
        dto.updatedAt = notice.getUpdatedAt();
        dto.viewCount = notice.getViewCount();
        dto.commentCount = commentCount;
        dto.position = Position.values()[notice.getPosition()].name();
        return dto;
    }

    public static NoticeResponse fromEntity(Notice notice, HrUserResponse user, DepResponse dep) {
        NoticeResponse dto = new NoticeResponse();
        dto.noticeId = notice.getNoticeId();
        dto.title = notice.getTitle();
        dto.content = notice.getContent();
        dto.name = user.getName();
        dto.departmentId = notice.getDepartmentId();
        dto.departmentName = dep.getName();
        dto.employStatus = user.getStatus(); // ✅ 직관적 매핑
        dto.employeeId = notice.getEmployeeId();
        dto.attachmentUri = notice.getAttachmentUri();
        dto.boardStatus = notice.isBoardStatus();
        dto.createdAt = notice.getCreatedAt();
        dto.updatedAt = notice.getUpdatedAt();
        dto.viewCount = notice.getViewCount();
        dto.position = Position.values()[notice.getPosition()].name();
        return dto;
    }

}
