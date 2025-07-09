package com.playdata.noticeservice.notice.dto;

import com.playdata.noticeservice.notice.entity.Notice;
import lombok.*;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.time.LocalDate;

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
    private Long departmentId;
    private boolean notice;
    private String attachmentUri;
    private boolean boardStatus;
    private LocalDate createdAt;
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

    public static NoticeResponse fromEntity(Notice notice, String name) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .employeeId(notice.getEmployeeId())
                .name(name) // 여기에 주입
                .departmentId(notice.getDepartmentId())
                .notice(notice.isNotice())
                .attachmentUri(notice.getAttachmentUri())
                .boardStatus(notice.isBoardStatus())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .viewCount(notice.getViewCount())
                .build();
    }

    public static NoticeResponse fromEntity(Notice notice, String name, String departmentName) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .employeeId(notice.getEmployeeId())
                .name(name) // 여기에 주입
                .departmentId(notice.getDepartmentId())
                .departmentName(departmentName)
                .notice(notice.isNotice())
                .attachmentUri(notice.getAttachmentUri())
                .boardStatus(notice.isBoardStatus())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .viewCount(notice.getViewCount())
                .build();
    }

}
