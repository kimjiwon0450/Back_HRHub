// com.playdata.common.enums.AlertMessage.java
package com.playdata.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertMessage {

    // ✅ 성공 메시지
    NOTICE_CREATE_SUCCESS("게시글이 성공적으로 작성되었습니다."),
    NOTICE_UPDATE_SUCCESS("게시글이 성공적으로 수정되었습니다."),
    NOTICE_DELETE_SUCCESS("게시글이 성공적으로 삭제되었습니다."),

    // ✅ 에러 메시지
    NOTICE_CREATE_FAILED("게시글 작성 에러가 발생하였습니다."),
    NOTICE_UPDATE_FAILED("게시글 수정 에러가 발생하였습니다."),
    NOTICE_DELETE_FAILED("게시글 삭제 에러가 발생하였습니다."),
    FILE_DOWNLOAD_FAILED("첨부파일 다운로드에 실패했습니다."),
    TOKEN_EXPIRED("로그인 세션이 만료되었습니다. 다시 로그인 해주세요.");

    private final String message;
}
