package com.playdata.noticeservice.common.dto;

import com.playdata.noticeservice.notice.entity.Notice;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class CommentCountResponse {
    private int commentCount;
}