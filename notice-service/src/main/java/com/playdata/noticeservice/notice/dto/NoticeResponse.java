package com.playdata.noticeservice.notice.dto;

import com.playdata.noticeservice.notice.entity.Notice;
import lombok.*;

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
    private Long writerId;
    private String writerName; // 작성자 이름
    private String writerDepartment;
    private Long departmentId;
    private boolean isNotice;
    private boolean hasAttachment;
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
                .isNotice(notice.isNotice())
                .hasAttachment(notice.isHasAttachment())
                .writerId(notice.getWriterId())
                .departmentId(notice.getDepartmentId())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .viewCount(notice.getViewCount())
                .build();
    }

    public static NoticeResponse fromEntity(Notice notice, String writerName) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .writerId(notice.getWriterId())
                .writerName(writerName) // 여기에 주입
                .departmentId(notice.getDepartmentId())
                .isNotice(notice.isNotice())
                .hasAttachment(notice.isHasAttachment())
                .boardStatus(notice.isBoardStatus())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .viewCount(notice.getViewCount())
                .build();
    }
}
