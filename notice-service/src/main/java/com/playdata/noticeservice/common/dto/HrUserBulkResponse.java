package com.playdata.noticeservice.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class HrUserBulkResponse extends CommonResDto<List<HrUserResponse>> {

}