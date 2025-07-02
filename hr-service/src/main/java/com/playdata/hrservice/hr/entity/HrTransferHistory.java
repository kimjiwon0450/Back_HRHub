package com.playdata.hrservice.hr.entity;

import com.playdata.hrservice.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class HrTransferHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employeeId")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "oldDepartmentId")
    private Department oldDepartment;

    @ManyToOne
    @JoinColumn(name = "newDepartmentId")
    private Department newDepartment;

    private String oldPosition;
    private String newPosition;
    private String memo;
}
