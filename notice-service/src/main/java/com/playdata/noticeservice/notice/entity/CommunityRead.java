package com.playdata.noticeservice.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tbl_community_read", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"community_id", "employee_id"})
})
public class CommunityRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long communityReadId;

    private Long communityId;

    private Long employeeId;

    private LocalDateTime readAt;
}
