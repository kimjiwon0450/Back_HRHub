package com.playdata.noticeservice.notice.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity

@Table(name = "tbl_notice")
public class Notice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private Long employeeId;
    private Long departmentId;

    private boolean published = false; // 게시 여부
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;   // 예약 시간

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String attachmentUri;
    private boolean boardStatus;
    private int viewCount = 0;
//    @Enumerated(EnumType.ORDINAL) // 👈 추가
    private int position;


    // Setter with enum
    public void setPosition(int position) {
        this.position = position;
    }

    // Getter with enum
    public Position getPositionEnum() {
        return Position.values()[this.position];
    }

    // Optional: position 필드 접근 getter도 유지 가능
    public int getPosition() {
        return this.position;
    }

}