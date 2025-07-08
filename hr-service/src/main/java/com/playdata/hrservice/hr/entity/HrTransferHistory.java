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

    /*
        [
         { "sequence_id" : 0,
            "department_id" : 10,
            "position_id" : 11,
             "memo" : "" } ,
            .....
           { "sequence_id" : 3,
            "department_id" : 30,
            "position_id" : 31,
             "memo" : "" }
        ]
    */
    @Column(columnDefinition = "json")
    private String transferHistory;

    public void updateTransferHistory(String transferHistory) {
        this.transferHistory = transferHistory;
    }
}
