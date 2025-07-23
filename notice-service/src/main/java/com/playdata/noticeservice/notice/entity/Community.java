package com.playdata.noticeservice.notice.entity;

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

@Table(name = "tbl_community")
public class Community {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long communityId;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private Long employeeId;
    private Long departmentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String attachmentUri;
    private boolean boardStatus;
    private int viewCount = 0;
}