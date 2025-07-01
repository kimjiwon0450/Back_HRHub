package com.playdata.hrservice.hr.entity;

import com.playdata.hrservice.common.entity.BaseTimeEntity;
import com.playdata.hrservice.hr.dto.DepartmentResDto;
import jakarta.persistence.*;
import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Department extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToOne
    @JoinColumn(name = "managerId")
    private Employee manager;

    public DepartmentResDto toDto() {
        return DepartmentResDto.builder()
                .id(id)
                .name(name)
                .build();
    }
}
