package com.playdata.noticeservice.common.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepResponse {
    private Long id;
    private String name;
}