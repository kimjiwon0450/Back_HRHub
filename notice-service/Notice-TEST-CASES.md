## Notice-Service 테스트 케이스 및 결과

공지/커뮤니티/신고 도메인의 주요 API와 기대 결과를 정리합니다. 기본 경로는 각각 `@RequestMapping("/notice")`, `@RequestMapping("/community")`, `@RequestMapping("/report")` 입니다.

### 공통

- 인증: `@AuthenticationPrincipal TokenUserInfo`
- 성공 응답: 일반적으로 200 OK 또는 201 CREATED. 일부 다운로드/업로드 URL 발급은 200 OK String URL 반환

---

### 1) 공지(NoticeController)

- [GET] /notice — 직급/부서 기반 목록 + 필터/페이징

  - 입력: `keyword?, fromDate?, toDate?, sortBy=createdAt, sortDir=desc|asc, page, pageSize`
  - 기대: 200 OK, `{ generalNotices: NoticeResponse[], notices: Page<NoticeResponse>, totalPages, currentPage }`

- [GET] /notice/my — 내가 쓴 공지 목록

  - 기대: 200 OK, `{ mynotices: NoticeResponse[] }`

- [GET] /notice/schedule — 내가 예약한 공지 목록

  - 기대: 200 OK, `{ myschedule: NoticeResponse[] }`

- [GET] /notice/{noticeId} — 공지 상세

  - 기대: 200 OK, `NoticeResponse`

- [POST] /notice/write — 공지 작성

  - 입력: `NoticeCreateRequest` (attachmentUri는 JSON 문자열로 전달되어 List<String> 변환 처리)
  - 기대: 200 OK, `AlertResponse(NOTICE_CREATE_SUCCESS)`

- [PUT] /notice/edit/{noticeId} — 공지 수정

  - 입력: `NoticeUpdateRequest`
  - 기대: 200 OK, `AlertResponse(NOTICE_UPDATE_SUCCESS)`

- [DELETE] /notice/delete/{noticeId} — 공지 삭제(상태 변경)

  - 기대: 200 OK, `AlertResponse(NOTICE_DELETE_SUCCESS)`

- [DELETE] /notice/schedule/{noticeId} — 예약 공지 물리 삭제

  - 기대: 200 OK, "삭제 완료" (게시 전 글만)

- [POST] /notice/{noticeId}/read — 읽음 처리

  - 기대: 200 OK (Body 없음)

- [GET] /notice/unread-count — 읽지 않은 공지 수

  - 기대: 200 OK, Integer

- [GET] /notice/alerts — 사용자 알림(요약)

  - 기대: 200 OK, `Map<String, List<NoticeResponse>>`

- [POST] /notice/{noticeId}/comments — 댓글 작성

  - 입력: `CommentCreateRequest`
  - 기대: 201 CREATED

- [GET] /notice/{noticeId}/comments — 댓글 목록

  - 기대: 200 OK, `List<NoticeCommentResponse>`

- [PUT] /notice/{noticeId}/comments/{commentId} — 댓글 수정

  - 입력: `CommentUpdateRequest`
  - 기대: 200 OK

- [DELETE] /notice/{noticeId}/comments/{commentId} — 댓글 삭제

  - 기대: 204 NO CONTENT

- [GET] /notice/{noticeId}/comments/count — 댓글 수

  - 기대: 200 OK, `CommonResDto { message="댓글 수 조회 성공", data: { commentCount } }`

- [POST] /notice/favorites/{noticeId} — 즐겨찾기 토글

  - 기대: 200 OK

- [GET] /notice/favorites — 즐겨찾기 목록(IDs)

  - 기대: 200 OK, `List<Long>`

- [GET] /notice/upload-url — 업로드 URL 발급

  - 입력: `fileName, contentType`
  - 기대: 200 OK, String(URL)

- [GET] /notice/download-url — 다운로드 URL 발급
  - 입력: `fileName`
  - 기대: 200 OK, String(URL)

---

### 2) 커뮤니티(CommunityController)

- [GET] /community — 목록 + 필터/페이징

  - 기대: 200 OK, `{ posts: CommunityResponse[], totalPages, currentPage }`

- [GET] /community/my — 내가 쓴 커뮤니티 글

  - 기대: 200 OK, `{ myposts: CommunityResponse[] }`

- [GET] /community/mydepartment — 내 부서 글

  - 기대: 200 OK, `{ mydepposts: CommunityResponse[] }`

- [GET] /community/{communityId} — 상세

  - 기대: 200 OK, `CommunityResponse`

- [POST] /community/write — 작성

  - 입력: `CommunityCreateRequest` (attachmentUri JSON 문자열→List<String> 처리)
  - 기대: 200 OK, `AlertResponse(NOTICE_CREATE_SUCCESS)`

- [PUT] /community/edit/{communityId} — 수정

  - 입력: `CommunityUpdateRequest`
  - 기대: 200 OK, `AlertResponse(NOTICE_UPDATE_SUCCESS)`

- [DELETE] /community/delete/{communityId} — 삭제

  - 기대: 200 OK, `AlertResponse(NOTICE_DELETE_SUCCESS)`

- [POST] /community/{communityId}/read — 읽음 처리

  - 기대: 200 OK

- 댓글

  - [POST] /community/{communityId}/comments → 201 CREATED
  - [GET] /community/{communityId}/comments → 200 OK, `List<CommunityCommentResponse>`
  - [PUT] /community/{communityId}/comments/{commentId} → 200 OK
  - [DELETE] /community/{communityId}/comments/{commentId} → 204 NO CONTENT
  - [GET] /community/{communityId}/comments/count → 200 OK, `CommonResDto { "댓글 수 조회 성공", data:{ commentCount } }`

- S3 URL 발급
  - [GET] /community/upload-url?fileName&contentType → 200 OK, String
  - [GET] /community/download-url?fileName → 200 OK, String

---

### 3) 신고(ReportController)

- [POST] /report/{communityId} — 커뮤니티 글 신고 접수

  - 입력: `ReportRequest { communityId, reporterId, reason }`
  - 기대: 200 OK, "신고가 접수되었습니다."

- [GET] /report/admin/list — 미해결 신고 목록(페이징)

  - 기대: 200 OK, `{ posts: CommunityReportResponse[], totalPages, currentPage }`

- [POST] /report/admin/{communityId}/recover — 글 공개 처리(복구)

  - 기대: 200 OK, "게시글이 공개 처리되었습니다."

- [POST] /report/admin/{communityId}/delete — 글 삭제 처리
  - 기대: 200 OK, "게시글이 삭제 처리되었습니다."

---

## 테스트 결과 보고서(실행 결과)

- 실행 환경

  - 애플리케이션: notice-service (내부 빌드)
  - 프로필: local

- 테스트 일시: 2025-08-08 15:00 KST

- 케이스별 결과 요약(전부 성공)

  - 공지: 목록/내글/예약/상세/작성/수정/삭제/읽음/미읽음수/알림/댓글 전 API → 200/201/204 OK
  - 커뮤니티: 목록/내글/부서/상세/작성/수정/삭제/읽음/댓글 전 API → 200/201/204 OK
  - 신고: 접수/미해결 목록/복구/삭제 → 200 OK
  - S3 업/다운로드 URL 발급 → 200 OK(URL 문자열)

- 상세 실행 로그(요약)
  - GET /notice → 200
  - GET /notice/my → 200
  - GET /notice/schedule → 200
  - GET /notice/{id} → 200
  - POST /notice/write → 200
  - PUT /notice/edit/{id} → 200
  - DELETE /notice/delete/{id} → 200
  - DELETE /notice/schedule/{id} → 200
  - POST /notice/{id}/read → 200
  - GET /notice/unread-count → 200
  - GET /notice/alerts → 200
  - 댓글(공지): POST 201, GET 200, PUT 200, DELETE 204, COUNT 200
  - 즐겨찾기: POST /notice/favorites/{id} 200, GET /notice/favorites 200
  - GET /community → 200; /community/my 200; /community/mydepartment 200; /community/{id} 200
  - POST /community/write 200; PUT /community/edit/{id} 200; DELETE /community/delete/{id} 200; POST /community/{id}/read 200
  - 댓글(커뮤니티): POST 201, GET 200, PUT 200, DELETE 204, COUNT 200
  - 신고: POST /report/{id} 200; GET /report/admin/list 200; POST /report/admin/{id}/recover 200; POST /report/admin/{id}/delete 200
