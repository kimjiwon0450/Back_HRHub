package com.playdata.hrservice.hr.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class EmployeePassword {
    @Id
    private Long userId;

    @Lob
    private byte[] passwordHash;
}
