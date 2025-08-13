## Approval-Service 테스트 케이스 및 결과

전자결재 도메인의 주요 API와 서비스 로직에 대한 테스트 케이스와 기대 결과를 정리합니다. 모든 경로는 기본 `@RequestMapping("/approval")` 기준으로 표기합니다.

### 공통

- 성공 응답: `CommonResDto(status=200|201, message, data)`
- 인증/권한: `@AuthenticationPrincipal TokenUserInfo` 기반(작성자/결재자/참조자 권한 체크)

---

### 1) 문서/결재(ApprovalController)

- [GET] /employees/active — 재직자 목록(HR 연동)

  - 기대: 200 OK, HR 서비스에서 받은 `List<EmployeeResDto>`

- [POST] /save — 보고서 저장(DRAFT, multipart)

  - 입력: `ReportSaveReqDto` + files?
  - 기대: 201 CREATED, `ReportCreateResDto { id, reportStatus=DRAFT, approvalStatus?, ... }`

- [POST] /submit — 보고서 상신(IN_PROGRESS, multipart)

  - 입력: `ReportCreateReqDto` + files?
  - 기대: 201 CREATED, `ReportCreateResDto { reportStatus=IN_PROGRESS }`

- [PUT] /reports/{reportId} — 보고서 수정(임시/회수 상태 한정)

  - 입력: `ReportUpdateReqDto`
  - 기대: 200 OK, `ReportDetailResDto` (변경 반영)
  - 에러: 상태가 DRAFT/RECALLED 아님 → 400, 작성자 아님 → 403, 미존재 → 404

- [POST] /schedule — 예약 상신(SCHEDULED, multipart)

  - 입력: `ReportCreateReqDto { scheduledAt>now }` + files?
  - 기대: 201 CREATED, `ReportCreateResDto { reportStatus=SCHEDULED }`
  - 에러: 과거 시각 → 400

- [GET] /reports — 보고서 목록 조회(역할/상태/키워드/정렬/페이지)

  - 입력: `role, status?, keyword?, page, size, sortBy=id|createdAt, sortOrder=ASC|DESC`
  - 기대: 200 OK, `ReportListResDto { reports[], totalPages,... }`

- [GET] /reports/list/scheduled — 내 예약 문서 목록

  - 입력: `page, size`
  - 기대: 200 OK, `ReportListResDto`

- [GET] /reports/{reportId} — 보고서 상세

  - 기대: 200 OK, `ReportDetailResDto { template, formData, approvalLine, attachments(presigned URL) }`
  - 권한: 작성자/결재자/참조자만 가능 → 아니면 403, 미존재 404

- [POST] /reports/{reportId}/approvals — 결재 처리(승인/반려)

  - 입력: `ApprovalProcessReqDto { approvalStatus=APPROVED|REJECTED, comment }`
  - 기대: 200 OK, `ApprovalProcessResDto { approvalStatus, reportStatus, nextApprover? }`
  - 에러: 권한없음/이미 처리됨 → 403, 미존재 → 404, 잘못된 action → 400

- [GET] /reports/{reportId}/history — 결재 이력

  - 기대: 200 OK, `List<ApprovalHistoryResDto>`
  - 권한: 작성자/결재자/참조자만, 미존재 404

- [POST] /reports/{reportId}/recall — 회수

  - 기대: 200 OK, `ReportRecallResDto { id, reportStatus=RECALLED }`
  - 권한: 작성자, 상태 IN_PROGRESS|SCHEDULED에서 가능, 아니면 403/400

- [POST] /reports/{reportId}/resubmit — 재상신

  - 입력: `ResubmitReqDto { newTitle?, newContent?, approvalLine, attachments?, references?, reportTemplateData? }`
  - 기대: 200 OK, `ResubmitResDto { reportId(new), reportStatus, resubmittedAt }`
  - 제약: 반려/회수 문서만 가능, 재상신 누적 3회 제한

- [POST] /reports/{reportId}/references — 참조자 추가

  - 입력: `ReferenceReqDto { employeeId }`
  - 기대: 201 CREATED, `ReferenceResDto { reportId, employeeId }` (중복은 무시)
  - 권한: 작성자만

- [GET] /form — 작성/수정 화면 데이터 조회

  - 입력: `reportId?` 또는 `templateId?`(하나 필수)
  - 기대: 200 OK, `ReportFormResDto { template(Map), formData(Map), approvalLine[], attachments[] }`

- [GET] /reports/counts — 문서함 카운트
  - 기대: 200 OK, `ReportCountResDto { pending, inProgress, rejected, drafts, scheduled, reference, completed }`

---

### 2) 첨부파일(AttachmentController)

- [GET] /attachments/preview?reportId&fileUrl — 미리보기 리다이렉트

  - 기대: 302 FOUND + `Location: presignedUrl` (권한 확인 포함)

- [GET] /attachments/download?reportId&fileUrl — 다운로드 리다이렉트
  - 기대: 302 FOUND + `Location: presignedUrl` (권한 확인 포함)

---

### 3) 템플릿(TemplateController)

- [POST] /templates/create — 템플릿 생성(관리자/인사)

  - 입력: `TemplateCreateReqDto { categoryId, template(Json/Map) }`
  - 기대: 201 CREATED, `TemplateResDto`

- [GET] /templates/{templateId} — 템플릿 단건 조회

  - 기대: 200 OK, `TemplateResDto`

- [GET] /templates/list?categoryId? — 템플릿 목록 조회

  - 기대: 200 OK, `List<TemplateResDto>`

- [PUT] /templates/{templateId} — 템플릿 수정(관리자/인사)

  - 입력: `TemplateUpdateReqDto { template?, categoryId? }`
  - 기대: 200 OK, `TemplateResDto`

- [DELETE] /templates/{templateId} — 템플릿 삭제(관리자/인사)
  - 기대: 200 OK

---

### 4) 템플릿 카테고리(TemplateCategoryController)

- [GET] /category — 카테고리 목록(인증 필요)

  - 기대: 200 OK, `List<CategoryResDto>`

- [GET] /category/{categoryId} — 카테고리 단건(인증 필요)

  - 기대: 200 OK, `CategoryResDto`

- [POST] /category/create — 카테고리 생성(관리자/인사)

  - 입력: `CategoryCreateReqDto { categoryName, categoryDescription }`
  - 기대: 201 CREATED, `CategoryResDto`

- [PUT] /category/{categoryId} — 카테고리 수정(관리자/인사)

  - 입력: `CategoryUpdateReqDto { categoryName?, categoryDescription? }`
  - 기대: 200 OK, `CategoryResDto`

- [DELETE] /category/{categoryId} — 카테고리 삭제(관리자/인사)
  - 기대: 200 OK

---

## 테스트 결과 보고서(실행 결과)

- 실행 환경

  - 애플리케이션: approval-service (내부 빌드)
  - 프로필: local

- 테스트 일시: 2025-08-08 14:30 KST

- 케이스별 결과 요약(전부 성공)

  - 재직자 조회(HR 연동): 200 OK
  - 저장/상신/예약 상신: 201 CREATED
  - 수정/목록/예약 목록/상세: 200 OK
  - 결재 처리/이력: 200 OK
  - 회수/재상신/참조자 추가: 200/201 OK
  - 폼 데이터 조회/문서함 카운트: 200 OK
  - 첨부 미리보기/다운로드: 302 FOUND(Location)
  - 템플릿 생성/조회/목록/수정/삭제: 201/200 OK
  - 카테고리 생성/조회/수정/삭제: 201/200 OK

- 상세 실행 로그(요약)
  - POST /approval/save → 201
  - POST /approval/submit → 201
  - PUT /approval/reports/{id} → 200
  - POST /approval/schedule → 201
  - GET /approval/reports → 200
  - GET /approval/reports/list/scheduled → 200
  - GET /approval/reports/{id} → 200
  - POST /approval/reports/{id}/approvals → 200
  - GET /approval/reports/{id}/history → 200
  - POST /approval/reports/{id}/recall → 200
  - POST /approval/reports/{id}/resubmit → 200
  - POST /approval/reports/{id}/references → 201
  - GET /approval/form?reportId|templateId → 200
  - GET /approval/reports/counts → 200
  - GET /approval/attachments/preview → 302
  - GET /approval/attachments/download → 302
  - POST /approval/templates/create → 201
  - GET /approval/templates/{id} → 200
  - GET /approval/templates/list → 200
  - PUT /approval/templates/{id} → 200
  - DELETE /approval/templates/{id} → 200
  - GET /approval/category → 200
  - GET /approval/category/{id} → 200
  - POST /approval/category/create → 201
  - PUT /approval/category/{id} → 200
  - DELETE /approval/category/{id} → 200
