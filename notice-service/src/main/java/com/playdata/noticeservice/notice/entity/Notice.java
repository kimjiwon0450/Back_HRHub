package com.playdata.noticeservice.notice.entity;

import jakarta.persistence.*;
import jakarta.validation.groups.Default;
import lombok.*;
import org.bouncycastle.jcajce.provider.drbg.DRBG;

import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity

@Table(name = "tbl_board")
public class Notice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private Long employeeId;
    private Long departmentId;
    private boolean notice; // 공지 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String attachmentUri;
    private boolean boardStatus;
    private int viewCount = 0;
    @Enumerated(EnumType.ORDINAL) // 👈 추가
    private int position;


    // Setter with enum
    public void setPosition(Position position) {
        this.position = position.ordinal();
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