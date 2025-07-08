package com.playdata.noticeservice.notice.entity;

import jakarta.persistence.*;
import jakarta.validation.groups.Default;
import lombok.*;
import org.bouncycastle.jcajce.provider.drbg.DRBG;

import java.time.LocalDateTime;
import java.time.LocalDate;


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
    private boolean isNotice; // 공지 여부
    private LocalDate createdAt;
    private LocalDateTime updatedAt;
    private boolean hasAttachment;
    private boolean boardStatus;
    private int viewCount = 0;


    @Column(columnDefinition = "TEXT")
    private String fileUrls; // 여러 개일 경우 ,로 구분된 문자열로 저장



    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}