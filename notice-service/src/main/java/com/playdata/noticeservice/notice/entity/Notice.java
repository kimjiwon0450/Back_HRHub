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





    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}