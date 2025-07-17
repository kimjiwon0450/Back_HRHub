// com.playdata.common.enums.AlertMessage.java
package com.playdata.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertMessage {

    // ✅ 성공 메시지
    LOGIN_SUCCESS("로그인에 성공했습니다."),
    EMAIL_VERIFICATION_SENT("인증 이메일이 발송되었습니다."),
    PASSWORD_RESET_SUCCESS("비밀번호 재설정이 완료되었습니다."),
    NOTICE_CREATE_SUCCESS("게시글이 성공적으로 작성되었습니다."),
    NOTICE_UPDATE_SUCCESS("게시글이 성공적으로 수정되었습니다."),
    NOTICE_DELETE_SUCCESS("게시글이 성공적으로 삭제되었습니다."),
    DEPARTMENT_REGISTER_SUCCESS("부서가 성공적으로 등록되었습니다."),
    EMPLOYEE_RETIRE_SUCCESS("퇴사 처리가 완료되었습니다."),
    APPROVAL_SUBMIT_SUCCESS("결재기안이 상신되었습니다."),
    APPROVAL_SAVE_SUCCESS("기안이 임시저장되었습니다."),
    APPROVAL_RECALL_SUCCESS("기안이 회수되었습니다."),
    TEMPLATE_CATEGORY_DELETE_SUCCESS("카테고리가 삭제되었습니다."),

    // ✅ 에러 메시지
    EMAIL_REQUIRED("이메일을 입력해주세요."),
    INVALID_EMAIL_FORMAT("이메일 형식이 올바르지 않습니다."),
    EMAIL_VERIFICATION_FAILED("인증 이메일 발송에 실패했습니다."),
    INVALID_PASSWORD("비밀번호가 올바르지 않습니다."),
    RETIRED_USER("이미 퇴사한 계정입니다."),
    PASSWORD_NOT_SET("비밀번호가 설정되지 않은 계정입니다."),
    PASSWORD_MISMATCH("비밀번호와 비밀번호 확인 값이 다릅니다."),
    PASSWORD_REQUIRED("비밀번호를 입력해주세요."),
    DUPLICATE_EMPLOYEE("이미 등록된 사원입니다."),
    DUPLICATE_DEPARTMENT("이미 존재하는 부서입니다."),
    DUPLICATE_EVALUATION("이미 평가한 인사입니다."),
    NOT_POST_OWNER("작성자가 아닌 사용자는 수정 또는 삭제할 수 없습니다."),
    FILE_DOWNLOAD_FAILED("첨부파일 다운로드에 실패했습니다."),
    TOKEN_EXPIRED("로그인 세션이 만료되었습니다. 다시 로그인 해주세요."),
    APPROVAL_REASON_REQUIRED("승인/반려 사유를 입력해주세요."),
    DEPARTMENT_NAME_REQUIRED("부서명을 입력해주세요.");

    private final String message;
}
