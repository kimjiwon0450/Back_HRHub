package com.playdata.approvalservice.approval.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ReportCountResDto {

    private final Long pending;
    private final Long inProgress;
    private final Long rejected;
    private final Long drafts;
    private final Long scheduled;
    private final Long completed;

    @JsonProperty("cc") // JSON 필드명을 'cc'로 지정
    private final Long reference;

    // JPA의 new 생성자 표현식을 위한 생성자
    public ReportCountResDto(Long pending, Long inProgress, Long rejected, Long drafts, Long scheduled, Long reference, Long completed) {
        this.pending = pending;
        this.inProgress = inProgress;
        this.rejected = rejected;
        this.drafts = drafts;
        this.scheduled = scheduled;
        this.reference = reference;
        this.completed = completed;
    }
}