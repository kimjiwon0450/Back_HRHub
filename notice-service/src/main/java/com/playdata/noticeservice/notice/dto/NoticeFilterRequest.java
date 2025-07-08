package com.playdata.noticeservice.notice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NoticeFilterRequest {

    private LocalDate fromDate;       // yyyy-MM-dd
    private LocalDate toDate;         // yyyy-MM-dd
    private String keyword;        // 제목/내용 검색
    private Long departmentId;     // 부서 ID
    private Boolean myPostsOnly;   // 내가 쓴 글만 보기
    private Integer page = 0;      // 기본 페이지 0
    private Integer size = 10;     // 기본 사이즈 10

}
