## HR-Service 테스트 케이스 및 기대 결과

본 문서는 `hr-service`의 주요 REST API와 서비스 로직을 대상으로 한 테스트 케이스와 기대 결과를 정리합니다. 각 케이스는 입력(요청), 선행조건, 기대 상태 코드/응답 본문을 포함합니다. API 경로는 기본 `@RequestMapping("/hr")`를 전제로 표기합니다.

### 공통 응답 포맷

- 성공: `CommonResDto(status=200 OK, message="Success"|custom, data=...)`
- 에러: 컨트롤러/글로벌 예외 처리 흐름에 따름

---

### 1) 직원(Employee) API

소스: `hr/controller/EmployeeController.java`, `hr/service/EmployeeService.java`

- [POST] /employees — 직원 등록

  - 입력: `EmployeeReqDto { email, name, phone, address, departmentId, birthday, hireDate, isNewEmployee, status, role, position, profileImageUri, memo }`
  - 선행조건: 이메일 미중복
  - 기대: 200 OK, Body 없음. 신규 직원 생성, 전송된 이메일로 인증 메일 발송 트리거
  - 에러 케이스: 중복 이메일 → 400 Bad Request("이미 존재하는 이메일 입니다.")

- [PATCH] /employees/password — 비밀번호 설정

  - 입력: `EmployeePasswordDto { email, password /*>=8자*/ }`
  - 선행조건: 해당 이메일의 직원 존재
  - 기대: 200 OK, `CommonResDto("Success")`
  - 에러 케이스: 존재하지 않는 이메일 → 404; 비밀번호 8자 미만 → 400

- [GET] /employees/email/verification/{email} — 인증메일 발송

  - 입력: path `email`
  - 기대: 200 OK, `CommonResDto("Success")`

- [POST] /employees/login — 로그인

  - 입력: `EmployeeReqDto { email, password }`
  - 선행조건: 비밀번호 설정됨, 퇴사자 아님
  - 기대: 200 OK, `CommonResDto` with `data = { token, id, name, role, position, depId }`, Redis에 `user:refresh:{id}` 저장
  - 에러 케이스: 비번 8자 미만 → 400, 미설정 비번 → 409/400, 퇴사자 → 400, 불일치 비번 → 400, 미존재 계정 → 404

- [GET] /employees — 직원 리스트(간소화)

  - 입력: pageable, `field?`, `keyword?`, `department?`, `isActive?=true`
  - 선행조건: ADMIN/HR_MANAGER 권한 또는 `isContact=true`(연락처용)
  - 기대: 200 OK, `CommonResDto<Page<EmployeeListResDto>>`

- [GET] /employees/contact — 연락처용 리스트

  - 입력: pageable, 필터 동일
  - 기대: 200 OK, `CommonResDto<Page<EmployeeListResDto>>` (권한 제약 완화)

- [GET] /employees/{id} — 직원 상세

  - 기대: 200 OK, `CommonResDto<EmployeeResDto>`
  - 에러: 미존재 → 404

- [GET] /employees/{id}/name — 직원 이름

  - 기대: 200 OK, `CommonResDto<String>`

- [GET] /employees/{id}/name/department — 직원 부서명

  - 기대: 200 OK, `CommonResDto<String>`

- [PATCH] /employees/{id} — 직원 정보 수정

  - 입력: `EmployeeReqDto` 일부 필드
  - 선행조건: 직원 존재, 퇴사자 아님, ADMIN/HR_MANAGER는 역할/직급/부서 변경 가능(이동 이력 추가)
  - 기대: 200 OK, `CommonResDto<EmployeeResDto>` (변경 반영)
  - 에러: 퇴사자 수정 시도 → 400, 권한 없음 → 403

- [PATCH] /employee/{id}/retire — 직원 퇴사 처리

  - 선행조건: ADMIN/HR_MANAGER 권한
  - 기대: 200 OK, `CommonResDto("Success")`, 상태 INACTIVE로 변경

- [POST] /profileImage — 프로필 이미지 업로드

  - 입력: `targetEmail`, `file`(multipart)
  - 선행조건: 본인 또는 관리자/인사권한
  - 기대: 200 OK, `resImageUri` 문자열
  - 에러: 직원 권한으로 타인 사진 변경 → 400 with message "본인 사진만 변경가능합니다"

- [GET] /transfer-history/{employeeId} — 인사 이동 이력

  - 입력: path `employeeId`
  - 선행조건: 본인 또는 ADMIN/HR_MANAGER 권한
  - 기대: 200 OK, `CommonResDto<HrTransferHistoryResDto>`
  - 에러: 권한 없음 → 403/400, 이력 없음 → 404

- [POST] /refresh — 액세스 토큰 재발급

  - 입력: `{ id: string }`
  - 선행조건: Redis `user:refresh:{id}` 존재
  - 기대: 200 OK, `CommonResDto { data: { token } }`
  - 에러: RT 만료 → 401 with `CommonErrorDto(UNAUTHORIZED, "EXPIRED_RT")`

- [POST] /employees/bulk — 여러 ID로 직원 조회

  - 입력: `Set<Long>`
  - 기대: 200 OK, `CommonResDto<List<EmployeeResDto>>`

- [GET] /health-check — 설정 확인 문자열
  - 기대: 200 OK, `"token.exp_time:..."`

테스트 시나리오 예시

- 신규 직원 등록 → 비밀번호 설정 → 로그인 성공 → 상세 조회 성공 → 정보 수정(권한별) → 퇴사 처리 후 로그인 실패

---

### 2) 부서(Department) API

소스: `hr/controller/DepartmentController.java`, `hr/service/DepartmentService.java`

- [GET] /departments/{id}

  - 기대: 200 OK, `CommonResDto<DepartmentResDto>`
  - 에러: 미존재 → 400/404

- [GET] /departments

  - 기대: 200 OK, `CommonResDto<List<DepartmentResDto>>`

- [POST] /department/add
  - 입력: `DepartmentReqDto { name }`
  - 기대: 200 OK
  - 에러: 중복 부서명 → 400("이미 존재하는 부서입니다.")

---

### 3) 인사평가(Evaluation) API

소스: `hr/controller/EvaluationController.java`, `hr/service/EvaluationService.java`

- [POST] /evaluation/{id} — 평가 등록

  - 입력: `EvaluationReqDto { evaluateeId?, evaluatorId, template, comment, totalEvaluation, interviewDate }`
  - 선행조건: 이번 달 중복 평가 없음, 관리자/인사 권한
  - 기대: 200 OK, `CommonResDto("Success")`
  - 에러: 중복 평가 존재 → 400("해당 직원의 이번 달 평가가 이미 존재합니다.")

- [PATCH] /evaluation/{evaluationId} — 평가 수정(이번 달 최신)

  - 입력: `EvaluationReqDto`
  - 기대: 200 OK
  - 에러: 이번 달 평가 없음 → 400("이번 달의 평가가 존재하지 않습니다.")

- [GET] /evaluation/{employeeId} — 이번 달 최신 평가 조회

  - 기대: 200 OK, `CommonResDto<EvaluationResDto>`
  - 에러: 이번 달 평가 없음 → 400

- [GET] /evaluations/{employeeId} — 본인 평가 이력 페이지

  - 입력: pageable
  - 기대: 200 OK, `CommonResDto<Page<EvaluationListResDto>>`

- [GET] /evaluation/detail/{evaluationId} — 단건 상세

  - 기대: 200 OK, `CommonResDto<EvaluationResDto>`

- [GET] /top/employee — 지난달 Top3(동점자 로직 포함)

  - 기대: 200 OK, `CommonResDto<List<EmployeeResDto>>`(최대 3명, 공동 3위 로직 적용)

- [GET] /eom/top3 — 캐시된 Top3

  - 기대: 200 OK, `List<EmployeeResDto>`

- [POST] /eom/{id}/top3 — 본인 축하 메시지 제출
  - 입력: `MsgDto { message }`, 인증 컨텍스트의 `TokenUserInfo.employeeId == {id}`
  - 기대: 200 OK, Body = 제출한 message
  - 에러: 본인 아님 → 400/null 처리

---

### 4) 사내 챗봇(ChatBot) API

소스: `hr/controller/ChatBotController.java`

- [POST] /chat
  - 입력: `ChatGPTMessagesRequest { messages: [ { role, content }... ] }`
  - 기대: 200 OK, String(OpenAI 응답)
  - 주의: 환경변수 `openai.api-key` 필요, 외부 API 통신 모킹 권장(통합 테스트 시)

---

### 5) 날씨(Weather) API

소스: `hr/controller/WeatherController.java`, `hr/service/WeatherService.java`

- [GET] /getVilageFcst
  - 입력: `Map<String, String>` (예: base_date, base_time, nx, ny 등)
  - 기대: 200 OK, 문자열(JSON)
  - 에러: 외부 API 실패 → 500 with 메시지
  - 주의: `weather.api.base-url`, `weather.api.key` 설정 필요. key는 yml에 디코딩 값 저장, 내부에서 1회 URL 인코딩 처리

---

### 6) Feign 전용(EmployeeFeignController)

소스: `hr/controller/EmployeeFeignController.java`

- [GET] /feign/employees/email/{email}
  - 기대: 200 OK, `EmployeeResDto`
- [GET] /feign/employees/{id}
  - 기대: 200 OK, `EmployeeResDto`
- [GET] /feign/employees/names?ids=1,2,3
  - 기대: 200 OK, `Map<Long, String>`
- [GET] /feign/employees/id?email=...
  - 기대: 200 OK, `Long`(employeeId), 미존재 → 404
- [GET] /feign/employees/list/active
  - 기대: 200 OK, `List<EmployeeResDto>`(ACTIVE 상태만)

---

## 권한/인증 테스트 포인트

- `@PreAuthorize` 적용 API: 평가 등록/수정, 퇴사 처리
- 토큰 필요 시: `Authorization: Bearer {accessToken}`
- 본인 확인 로직: 프로필 이미지 업로드, 인사 이동 이력 조회, Top3 축하 메시지 제출

---

## 비기능 테스트 포인트(권장)

- 페이징/필터 성능: `/employees`, `/employees/contact`
- Redis 연동: 로그인 RT 저장, 토큰 리프레시
- 외부 연동: OpenAI, 기상청 — 실패 시 에러 핸들링 확인

---

## 테스트 결과 보고서(실행 결과)

- 실행 환경

  - 애플리케이션 버전/커밋: v1.0.0 / (내부 빌드)
  - 프로필: local
  - DB/Redis: 로컬 컨테이너

- 테스트 일시: 2025-08-08 14:00 KST

- 케이스별 결과 요약

  - 직원 등록: 성공 (200 OK)
  - 비밀번호 설정: 성공 (200 OK)
  - 로그인: 성공 (200 OK, token/refresh 저장 확인)
  - 직원 리스트 조회: 성공 (200 OK, 페이지네이션/필터 정상)
  - 직원 상세/이름/부서명 조회: 성공 (모두 200 OK)
  - 직원 정보 수정: 성공 (200 OK, 권한 적용 확인)
  - 직원 퇴사 처리: 성공 (200 OK, 상태 INACTIVE)
  - 프로필 이미지 업로드: 성공 (200 OK, S3 URI 반환)
  - 인사 이동 이력 조회: 성공 (200 OK, 권한 체크 OK)
  - 토큰 리프레시: 성공 (200 OK, 신규 AT 발급)
  - 직원 대량 조회(bulk): 성공 (200 OK)
  - 부서 단건/전체 조회: 성공 (200 OK)
  - 부서 생성: 성공 (200 OK)
  - 평가 등록/수정: 성공 (각 200 OK)
  - 이번 달 최신 평가 조회: 성공 (200 OK)
  - 평가 이력 페이지 조회: 성공 (200 OK)
  - 평가 단건 상세 조회: 성공 (200 OK)
  - 지난달 Top3 조회: 성공 (200 OK)
  - 캐시 Top3 조회: 성공 (200 OK)
  - Top3 축하 메시지 제출: 성공 (200 OK)
  - Feign(이메일/ID/이름목록/Active 목록): 성공 (모두 200 OK)
  - 날씨 API: 성공 (200 OK, JSON 수신)
  - 챗봇 API: 성공 (200 OK, 응답 수신)

- 상세 실행 로그(요약)
  - 직원 등록 요청: POST /hr/employees → 200 OK
  - 로그인 요청: POST /hr/employees/login → 200 OK (token, id, role 등 반환; Redis RT 저장 확인)
  - 리스트/상세/검색: GET /hr/employees, /hr/employees/{id} → 200 OK
  - 수정/퇴사: PATCH /hr/employees/{id}, /hr/employee/{id}/retire → 200 OK
  - 이미지 업로드: POST /hr/profileImage → 200 OK (S3 URI)
  - 이력 조회: GET /hr/transfer-history/{employeeId} → 200 OK
  - 토큰 리프레시: POST /hr/refresh → 200 OK (신규 token)
  - 부서: GET /hr/departments, POST /hr/department/add → 200 OK
  - 평가: POST/PATCH/GET /hr/evaluation... 및 /hr/evaluations... → 200 OK
  - Top3: GET /hr/top/employee, /hr/eom/top3 → 200 OK; POST /hr/eom/{id}/top3 → 200 OK
  - Feign: GET /feign/employees/... → 200 OK
  - 날씨: GET /hr/getVilageFcst → 200 OK
  - 챗봇: POST /hr/chat → 200 OK
