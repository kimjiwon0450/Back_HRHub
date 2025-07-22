package com.playdata.approvalservice.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class BaseTimeEntity {

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime reportCreatedAt;
}
