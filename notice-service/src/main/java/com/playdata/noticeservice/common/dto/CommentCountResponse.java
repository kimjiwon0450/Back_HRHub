package com.playdata.noticeservice.common.dto;

import com.playdata.noticeservice.notice.entity.Notice;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCountResponse {
    private int commentCount;
}