package com.playdata.approvalservice.approval.dto.response;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResDto {
    private Long commentId;
    private Long reportId;
    private Long authorId;
    private String comment;
    private LocalDateTime createdAt;
}