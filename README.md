# HRHub 백엔드 설계 문서 (2팀-DCT)

## 공통 사항

### 인증 및 인가

- JWT 기반 인증, Spring Security 적용
- 모든 요청은 Gateway에서 인증 토큰 확인 후 해당 서비스로 전달

### 서비스 간 통신

- Feign + Spring Cloud Eureka 사용
- 서비스 간 호출 시 JWT 토큰 전달

### 테스트 환경 정보

- OS: Window11
- DB: MySQL
- Java: OpenJDK 17
- Spring Boot: 3.5.4
- 테스트 툴: Postman,  Swagger UI
- 배포: Docker Compose 기반 로컬 테스트 환경

---

### 인터페이스 설계서

| 호출 서비스 | 제공 서비스 | HTTP 메서드 | 엔드포인트 | 요청 파라미터 | 응답 데이터/타입 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| approval-service | hr-service | GET | `/feign/employees/{id}` | `id` (PathVariable) | `EmployeeResDto` (직원 상세 정보) | 직원 ID로 단건 조회 |
| approval-service | hr-service | GET | `/feign/employees/email/{email}` | `email` (PathVariable) | `EmployeeResDto` (직원 상세 정보) | 이메일로 단건 조회 |
| approval-service | hr-service | GET | `/feign/employees/names` | `ids` (RequestParam, `List<Long>`) | `Map<Long, String>` (ID–이름 매핑) | 다수 직원 이름 일괄 조회 |
| approval-service | hr-service | GET | `/feign/employees/id` | `email` (RequestParam) | `Long` (직원 ID) | 이메일로 ID 조회 |
| approval-service | hr-service | GET | `/feign/employees/list/active` | 없음 | `List<EmployeeResDto>` (재직 중 직원 목록) | 결재선/참조자 선택용 |

---

## 1. HR 서비스

### 기능 개요

- 직원 관리, 조직 관리 등을 담당하는 핵심 서비스

### 주요 Entity

- `Employee (직원)`
- `Department (부서)`
- `EmployeePassword (직원 바말번호)`
- `Evaluation (인사평가)`
- `HrTransferHistory (인사이동기록)`

### Sequence 다이어그램

- 1. 로그인
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant HR as HR 서비스
      participant DB as DB
    
      FE->>GW: POST /api/login (ID/PW)
      GW->>HR: 인증 요청 전달
      HR->>DB: 사용자 검증
      DB-->>HR: 사용자 정보 반환
      HR-->>GW: JWT 발급
      GW-->>FE: 로그인 성공 및 토큰 반환
    
    ```
    
- 2. 직원 등록
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant HR as HR 서비스
      participant DB as DB
    
      FE->>GW: POST /api/employees
      GW->>HR: 등록 요청 전달
      HR->>DB: 직원 정보 저장
      HR-->>GW: 등록 완료 응답
      GW-->>FE: 성공 메시지
    
    ```
    
- 3. 직원 조회
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant HR as HR 서비스
      participant DB as DB
    
      FE->>GW: GET /api/employees
      GW->>HR: 요청 전달
      HR->>DB: 직원 리스트 조회
      DB-->>HR: 직원 목록 반환
      HR-->>GW: 데이터 반환
      GW-->>FE: 직원 목록 출력
    
    ```
    
- 4. 부서 등록
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant HR as HR 서비스
      participant DB as DB
    
      FE->>GW: POST /api/departments
      GW->>HR: 요청 전달
      HR->>DB: 부서 저장
      HR-->>GW: 등록 결과 반환
      GW-->>FE: 완료 메시지
    
    ```
    
- 5. 인사평가 등록
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant HR as HR 서비스
      participant DB as DB
    
      FE->>GW: POST /api/evaluations
      GW->>HR: 평가 등록 요청
      HR->>DB: 인사평가 데이터 저장
      HR-->>GW: 등록 완료 응답
      GW-->>FE: 완료 메시지
    
    ```
    
- 6. 인사평가 조회
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant HR as HR 서비스
      participant DB as DB
    
      FE->>GW: GET /api/evaluations/my
      GW->>HR: 로그인 토큰 전달
      HR->>DB: 로그인 사용자의 인사평가 조회
      DB-->>HR: 평가 결과 반환
      HR-->>GW: 평가 결과 응답
      GW-->>FE: 평가 결과 출력
    
    ```
    

### UI 연동 흐름도

- 1. 로그인
    
    ```mermaid
    flowchart TD
      A[사용자 - 로그인 페이지 접속] --> B[ID/PW 입력 후
       로그인 버튼 클릭]
      B --> C[프론트엔드 API 호출]
      C --> D[게이트웨이에서 인증요청 전달]
      D --> E[HR 서비스 사용자 검증 및 토큰 발급]
      E --> F[토큰 수신 및 홈 화면 이동]
    
    ```
    
- 2. 직원 등록
    
    ```mermaid
    flowchart TD
      A[관리자 - 직원 등록 화면 진입] --> B[직원 정보 입력 후
       등록 버튼 클릭]
      B --> C[프론트엔드 API 호출]
      C --> D[게이트웨이 -> HR 서비스 호출]
      D --> E[DB에 직원 정보 저장]
      E --> F[등록 결과 반환 및 알림 출력]
    
    ```
    
- 3. 직원 목록 조회
    
    ```mermaid
    flowchart TD
      A[사용자 - 직원 목록 페이지 접근] --> B[프론트엔드 API 호출]
      B --> C[게이트웨이 -> HR 서비스 호출]
      C --> D[직원 목록 DB 조회]
      D --> E[프론트에 결과 데이터 출력]
    
    ```
    
- 4. 부서 등록
    
    ```mermaid
    flowchart TD
      A[관리자 - 부서 관리 페이지 진입] --> B[부서명 입력 후 등록 클릭]
      B --> C[API 호출을 통해 
      HR 서비스 요청]
      C --> D[부서 정보 DB 저장]
      D --> E[등록 성공 메시지 출력]
    
    ```
    
- 5. 인사평가 등록
    
    ```mermaid
    flowchart TD
      A[관리자 - 인사평가 등록 화면
       접속] --> B[평가 정보 입력 후 등록]
      B --> C[API 호출로 HR 서비스 전달]
      C --> D[DB에 평가 결과 저장]
      D --> E[등록 완료 메시지 출력]
    
    ```
    
- 6. 인사평가 조회
    
    ```mermaid
    flowchart TD
      A[직원 - 내 인사평가 
      조회 화면 접속] --> B[프론트엔드 API 호출]
      B --> C[게이트웨이에서 JWT 기반
       요청 전달]
      C --> D[HR 서비스 - 
       로그인 사용자 식별]
      D --> E[DB에서 본인의 평가 조회]
      E --> F[결과 반환 및 화면 출력]
    
    ```
    

### 테스트 케이스

| 테스트 ID | 테스트 항목 | 입력값 | 예상 결과 |
| --- | --- | --- | --- |
| TC_HR_001 | 로그인 | ID: test01 / PW: 1234 | JWT 토큰 발급 및 홈 이동 |
| TC_HR_002 | 로그인 실패 | ID: test01 / PW: wrong | 로그인 실패 메시지 출력 |
| TC_HR_003 | 직원 등록 | 이름: 홍갈동/ 부서: 개발팀 | 등록 완료 메시지 출력 |
| TC_HR_004 | 직원 목록 조회 | - | 전체 직원 목록 반환 |
| TC_HR_005 | 부서 등록 | 부서명: 기획팀 | 부서 등록 완료 메시지 |
| TC_HR_006 | 인사평가 등록 | 직원 ID: 3, 평균점수:3.5  | 등록 성공 메시지 출력 |
| TC_HR_007 | 인사평가 조회 | 로그인 사용자 본인 | 본인의 인사평가만 반환 |
| TC_HR_008 | 직원 정보 수정 | ID: 3 / 전화번호 수정 | 수정 성공 메시지 출력 |
| TC_HR_009 | 프로필 사진 업로드 | 직원 ID: 3 / 이미지 파일(jpg) | 업로드 성공 메시지 출력 |

---

### 테스트 결과 요약

| 테스트 ID | 테스트 항목 | 결과 | 비고 |
| --- | --- | --- | --- |
| TC_HR_001 | 로그인 | 성공 ✅ | 정상 로그인 처리 확인 |
| TC_HR_002 | 로그인 실패 | 성공 ✅ | 실패 메시지 정상 출력 |
| TC_HR_003 | 직원 등록 | 성공 ✅ | 필수값 입력 유효성 확인 |
| TC_HR_004 | 직원 목록 조회 | 성공 ✅ | 전체 리스트 정상 출력 |
| TC_HR_005 | 부서 등록 | 성공 ✅ | 중복 부서명 검사 완료 |
| TC_HR_006 | 인사평가 등록 | 성공 ✅ | 입력값 유효성 및 저장 확인 |
| TC_HR_007 | 인사평가 조회 | 성공 ✅ | 로그인 사용자 필터링 정상 작동 |
| TC_HR_008 | 직원 정보 수정 | 성공 ✅ | 정보 수정 및 반영 정상 처리 확인 |
| TC_HR_009 | 프로필 사진 업로드 | 성공 ✅ | 이미지 파일 업로드 정상 동작 |

[GitHub Repository - HR Service](https://github.com/ehgus8/petwiz-back/tree/main/hr-service)

---

## 2. 공지 서비스

### 기능 개요

- 사내 공지사항 작성, 조회, 수정, 삭제 기능 제공
- 직급별로 볼 수 있는 공지사항이 다른 레벨링 기능
- 관리자 전용 작성/수정 권한 부여

### 주요 Entity

- `Notice (공지글)`
- `NoticeRead (공지글의 읽음처리)`
- `NoticeComment (공지글의 댓글)`
- `Community (게시글)`
- `CommunityRead (게시글의 읽음처리)`
- `CommunityComment (게시글의 댓글)`
- `CommunityReport (신고된 게시글)`

### Sequence 다이어그램

- 1. 공지글 작성 (권한 체크)
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Notice as 공지 서비스
      participant DB as DB
    
      FE->>GW: POST /api/notices
      GW->>Notice: 사용자 Role, Position 확인
      alt 관리자 또는 MANAGER 이상
        Notice->>DB: 공지글 저장
        Notice-->>GW: 등록 완료
      else 권한 없음
        Notice-->>GW: 접근 거부 오류
      end
      GW-->>FE: 결과 반환
    
    ```
    
- 2. 공지글 조회 (Position 기반 필터링)
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Notice as 공지 서비스
      participant DB as DB
    
      FE->>GW: GET /api/notices
      GW->>Notice: 요청 전달
      Notice->>DB: 로그인 사용자의 Position 확인 후 적합한 공지 조회
      DB-->>Notice: 공지 리스트 반환
      Notice-->>GW: 데이터 반환
      GW-->>FE: 화면에 출력
    
    ```
    
- 3. 게시글 신고 및 처리
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Notice as 공지 서비스
      participant DB as DB
    
      FE->>GW: POST /api/notices/{id}/report
      GW->>Notice: 신고 접수 요청
      Notice->>DB: 신고 테이블에 기록
      DB-->>Notice: 저장 완료
      Notice-->>GW: 완료 메시지
      GW-->>FE: 완료 응답
    
      alt 관리자 화면에서
        FE->>GW: PATCH /api/reports/{id}/recover (or DELETE)
        GW->>Notice: 처리 요청
        Notice->>DB: 복구 또는 삭제 처리
        Notice-->>GW: 처리 완료
        GW-->>FE: 완료 메시지
      end
    
    ```
    
- 4. 댓글 작성
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Notice as 공지 서비스
      participant DB as DB
    
      FE->>GW: POST /api/notices/{noticeId}/comments
      GW->>Notice: 댓글 작성 요청 및 인증 토큰 검증
      Notice->>DB: 댓글 저장 (중복 여부, 내용 유효성 검증 포함)
      DB-->>Notice: 저장 완료
      Notice-->>GW: 등록 완료 응답
      GW-->>FE: 결과 반환
    
    ```
    

### UI 연동 흐름도

- 1. 공지글 작성 (권한 체크)
    
    ```mermaid
    flowchart TD
      A[관리자 - 공지 작성 화면 진입] --> B[공지 내용 입력 후 등록 클릭]
      B --> C[API 호출: /api/notices]
      C --> D[권한 체크 및 저장 처리]
      D --> E[등록 성공 메시지]
    
    ```
    
- 2. 공지글 조회 (Position 기반)
    
    ```mermaid
    flowchart TD
      A[사용자 - 공지 페이지 접속] --> B[API 호출: /api/notices]
      B --> C[로그인 사용자 Position 확인]
      C --> D[공지글 필터링 후 반환]
      D --> E[프론트에 출력]
    
    ```
    
- 3.게시글 신고 및 처
    
    ```mermaid
    flowchart TD
      subgraph UserReport [사용자 - 게시글 신고]
        A1[게시글 신고 버튼 클릭] --> A2[신고 사유 선택 및 입력]
        A2 --> A3[API 호출: POST 
        /api/notices/id/report]
        A3 --> A4[신고 중복 검사 및 처리]
        A4 --> A5[신고 완료 메시지 또는
         중복 오류 메시지]
      end
    
      subgraph AdminReport [관리자 - 신고 처리]
        B1[신고 목록 화면 진입] --> B2[신고 목록 API 호출]
        B2 --> B3[신고 데이터 화면 출력]
        B3 --> B4[복구 또는 삭제 처리 버튼 클릭]
        B4 --> B5[API 호출: PATCH
        /api/reports/id/recover 
        or DELETE]
        B5 --> B6[신고 상태 변경 및 처리 완료]
        B6 --> B7[처리 완료 메시지 및 신고 목록 갱신]
      end
    
    ```
    
- 4.댓글 작성
    
    ```mermaid
    flowchart TD
      CW1[사용자 - 공지 상세 화면 접속] --> CW2[댓글 입력 후 등록 클릭]
      CW2 --> CW3[API 호출: POST 
      /api/notices/id/comments]
      CW3 --> CW4[댓글 유효성 검사 및 저장]
      CW4 --> CW5[댓글 목록 갱신 및
       화면 업데이트]
    
    ```
    

### 테스트 케이스

| 테스트 ID | 테스트 항목 | 입력값 | 예상 결과 |
| --- | --- | --- | --- |
| TC_NOTICE_001 | 공지 등록 | 제목+본문 입력 (관리자) | 등록 성공 메시지 출력 |
| TC_NOTICE_002 | 권한 없음 | 일반 사용자 권한으로 작성 요청 | 403 접근 거부 메시지 출력 |
| TC_NOTICE_003 | 공지 목록 조회 | 로그인 사용자 (Position: MANAGER) | 해당 Position 기준 공지 리스트 출력 |
| TC_NOTICE_004 | 댓글 작성 | 로그인 사용자, 댓글 입력 | 댓글 등록 완료 메시지 출력 |
| TC_NOTICE_005 | 댓글 작성 실패 | 비로그인 사용자, 댓글 입력 | 401 인증 실패 메시지 출력 |
| TC_NOTICE_006 | 게시글 신고 | 정상 사용자, 신고 사유 선택 | 신고 접수 성공 메시지 출력 |
| TC_NOTICE_007 | 중복 신고 | 동일 사용자가 같은 게시글 재신고 시도 | 중복 신고 메시지 (409 Conflict) 출력 |
| TC_NOTICE_008 | 관리자 - 신고 처리 복구 | 관리자 권한, 복구 요청 | 신고 상태 '복구 완료' 메시지 출력 |
| TC_NOTICE_009 | 관리자 - 신고 처리 삭제 | 관리자 권한, 삭제 요청 | 신고 상태 '삭제 완료' 메시지 출력 |
| TC_NOTICE_010 | 신고 처리 권한 없음 | 일반 사용자가 복구/삭제 요청 | 403 권한 없음 메시지 출력 |

### 테스트 결과 요약

| 테스트 ID | 테스트 항목 | 결과 | 비고 |
| --- | --- | --- | --- |
| TC_NOTICE_001 | 공지 등록 | 성공 ✅ | 관리자 권한 공지 등록 정상 작동 확인 |
| TC_NOTICE_002 | 권한 없음 | 성공 ✅ | 일반 사용자 접근 거부 확인 |
| TC_NOTICE_003 | 공지 목록 조회 | 성공 ✅ | Position 필터링 정상 동작 |
| TC_NOTICE_004 | 댓글 작성 | 성공 ✅ | 댓글 저장 및 목록 갱신 확인 |
| TC_NOTICE_005 | 댓글 작성 실패 | 성공 ✅ | 인증되지 않은 사용자 차단 확인 |
| TC_NOTICE_006 | 게시글 신고 | 성공 ✅ | 정상 신고 접수 확인 |
| TC_NOTICE_007 | 중복 신고 | 성공 ✅ | 중복 신고 차단 및 메시지 처리 확인 |
| TC_NOTICE_008 | 관리자 - 신고 처리 복구 | 성공 ✅ | 신고 상태 변경 확인 (복구) |
| TC_NOTICE_009 | 관리자 - 신고 처리 삭제 | 성공 ✅ | 신고 상태 변경 확인 (삭제) |
| TC_NOTICE_010 | 신고 처리 권한 없음 | 성공 ✅ | 일반 사용자 처리 요청 차단 확인 |

[GitHub Repository - Notice Service](https://github.com/ehgus8/petwiz-back/tree/main/notice-service)

---

## 3. 결재 서비스

### 기능 개요

- 전자결재 양식 및 템플릿 생성
- 결재 상신, 승인/반려 등 결재 처리 전반 담당

### 주요 Entity

- `ApprovalLine (결재선)`
- `ApprovalReferences (결재참조자)`
- `Reports (결재문서)`
- `ReportsTemplate (결재문서 템플릿)`
- `TemplateCategory (템플릿 카케고리)`

### Sequence 다이어그램

- 1.결재문서 작성 및 상신
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Approval as 결재 서비스
      participant DB as DB
    
      FE->>GW: POST /api/approvals
      GW->>Approval: 결재문서 상신 요청 (작성자, 결재선 포함)
      Approval->>DB: 결재문서 저장 (상태: 진행중)
      DB-->>Approval: 저장 완료
      Approval-->>GW: 상신 완료 응답
      GW-->>FE: 처리 결과 표시
    
    ```
    
- 2.결재 요청 처리 (승인 / 반려)
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Approval as 결재 서비스
      participant DB as DB
    
      FE->>GW: PATCH /api/approvals/{id} (승인 or 반려)
      GW->>Approval: 결재 처리 요청
      Approval->>DB: 결재 상태 업데이트
      DB-->>Approval: 저장 완료
      Approval-->>GW: 처리 완료
      GW-->>FE: 처리 결과 메시지 반환
    
    ```
    
- 3. 상신한 문서 회수
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Approval as 결재 서비스
      participant DB as DB
    
      FE->>GW: PATCH /api/approvals/{id}/recall
      GW->>Approval: 회수 요청
      Approval->>DB: 상태: 회수됨 으로 변경
      DB-->>Approval: 저장 완료
      Approval-->>GW: 회수 완료
      GW-->>FE: 회수 성공 메시지
    
    ```
    
- 4. 결재 양식 생성 (관리자만 가능)
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Approval as 결재 서비스
      participant DB as DB
    
      FE->>GW: POST /api/forms
      GW->>Approval: 사용자 Role 확인
      alt 관리자일 경우
        Approval->>DB: 양식 저장
        DB-->>Approval: 저장 완료
        Approval-->>GW: 생성 완료
      else 일반 사용자
        Approval-->>GW: 403 권한 오류
      end
      GW-->>FE: 응답 반환
    
    ```
    
- 5. 문서 수정 및 재상신 (임시저장 / 반려 / 회수 상태)
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Approval as 결재 서비스
      participant DB as DB
    
      FE->>GW: PUT /api/approvals/{id}
      GW->>Approval: 수정 요청
      Approval->>DB: 상태 확인 → 수정 가능 여부 확인
      Approval->>DB: 내용 수정 및 상태: 진행중으로 변경
      DB-->>Approval: 수정 완료
      Approval-->>GW: 재상신 완료
      GW-->>FE: 결과 메시지
    
    ```
    
- 6. 결재문서 조회
    
    ```mermaid
    sequenceDiagram
      participant FE as 프론트엔드
      participant GW as API Gateway
      participant Approval as 결재 서비스
      participant DB as DB
    
      FE->>GW: GET /api/approvals or /api/approvals/{id}
      GW->>Approval: 요청 전달
      Approval->>DB: 사용자 기준 문서 목록 or 상세 조회
      DB-->>Approval: 문서 데이터 반환
      Approval-->>GW: 결과 응답
      GW-->>FE: 목록 or 상세 화면 렌더링
    
    ```
    

### UI 연동 흐름도

- 1.결재문서 작성 및 상신
    
    ```mermaid
    flowchart TD
      A1[사용자 - 결재 작성 화면 접속] --> A2[결재 내용 입력]
      A2 --> A3[결재선 지정 및 상신 클릭]
      A3 --> A4[API 호출: POST 
      /api/approvals]
      A4 --> A5[결재문서 저장 및 처리]
      A5 --> A6[상신 완료 메시지 출력]
    
    ```
    
- 2.결재 요청 처리 (승인 / 반려)
    
    ```mermaid
    flowchart TD
      B1[결재자 - 결재 요청 문서 확인] --> B2[승인 또는 반려 버튼 클릭]
      B2 --> B3[API 호출: PATCH 
      /api/approvals/id]
      B3 --> B4[결과 처리 및 상태 변경]
      B4 --> B5[처리 완료 메시지]
    
    ```
    
- 3. 상신한 문서 회수
    
    ```mermaid
    flowchart TD
      C1[작성자 - 상신한 문서 확인] --> C2[회수 버튼 클릭]
      C2 --> C3[API 호출: PATCH 
      /api/approvals/id/recall]
      C3 --> C4[상태 회수됨으로 변경]
      C4 --> C5[회수 성공 메시지 출력]
    
    ```
    
- 4. 결재 양식 생성 (관리자만 가능)
    
    ```mermaid
    flowchart TD
      D1[관리자 - 양식 관리 화면 진입] --> D2[양식 작성 후 저장 클릭]
      D2 --> D3[API 호출: POST /api/forms]
      D3 --> D4[권한 확인 및 양식 저장]
      D4 --> D5[생성 성공 또는 
      권한 오류 메시지 출력]
    
    ```
    
- 5. 문서 수정 및 재상신 (임시저장 / 반려 / 회수 상태)
    
    ```mermaid
    flowchart TD
      E1[작성자 - 임시저장/반려/
      회수 문서 진입] --> E2[수정 후 재상신 클릭]
      E2 --> E3[API 호출: PUT 
      /api/approvals/id]
      E3 --> E4[수정 후 상태: 진행중 변경]
      E4 --> E5[재상신 성공 메시지]
    
    ```
    
- 6. 결재문서 조회
    
    ```mermaid
    flowchart TD
      F1[사용자 - 결재함 메뉴 접속] --> F2[API 호출: GET /api/approvals]
      F2 --> F3[사용자 기준 문서 리스트 반환]
      F3 --> F4[결과 렌더링 및 확인]
    
    ```
    

### 테스트 케이스

| 테스트 ID | 테스트 항목 | 입력값 | 예상 결과 |
| --- | --- | --- | --- |
| TC_APV_001 | 결재문서 작성 및 상신 | 제목, 내용, 결재선 입력 | 상신 완료 메시지 출력 |
| TC_APV_002 | 결재 승인 처리 | 승인 클릭 | 결재 상태: 승인 완료, 처리 성공 메시지 출력 |
| TC_APV_003 | 결재 반려 처리 | 반려 클릭, 반려 사유 입력 | 결재 상태: 반려, 처리 성공 메시지 출력 |
| TC_APV_004 | 문서 회수 | 회수 클릭 | 상태: 회수됨, 회수 성공 메시지 출력 |
| TC_APV_005 | 관리자 - 양식 생성 | 관리자 권한, 양식 제목 및 항목 입력 | 생성 완료 메시지 출력 |
| TC_APV_006 | 일반 사용자 양식 생성 시도 | 일반 권한으로 양식 생성 요청 | 403 권한 없음 메시지 출력 |
| TC_APV_007 | 반려 문서 재상신 | 반려 문서 수정 후 재상신 | 상태: 진행중, 재상신 성공 메시지 출력 |
| TC_APV_008 | 결재문서 조회 | 로그인 사용자 기준 목록 조회 | 해당 문서 리스트 반환 |
| TC_APV_009 | 결재문서 상세 조회 | 문서 ID로 상세조회 | 문서 상세 정보 반환 |

### 테스트 결과 요약

| 테스트 ID | 테스트 항목 | 결과 | 비고 |
| --- | --- | --- | --- |
| TC_APV_001 | 결재문서 작성 및 상신 | 성공 ✅ | 문서 저장 및 결재선 등록 확인 |
| TC_APV_002 | 결재 승인 처리 | 성공 ✅ | 상태 '승인' 변경 확인 |
| TC_APV_003 | 결재 반려 처리 | 성공 ✅ | 상태 '반려' 변경 및 반려 사유 저장 확인 |
| TC_APV_004 | 문서 회수 | 성공 ✅ | 회수 상태 처리 정상 작동 확인 |
| TC_APV_005 | 관리자 - 양식 생성 | 성공 ✅ | 관리자 권한으로 생성 정상 작동 |
| TC_APV_006 | 일반 사용자 양식 생성 시도 | 성공 ✅ | 권한 오류 메시지 처리 정상 확인 |
| TC_APV_007 | 반려 문서 재상신 | 성공 ✅ | 수정 및 상태 변경 정상 동작 확인 |
| TC_APV_008 | 결재문서 조회 | 성공 ✅ | 사용자 기준 필터링 정상 작동 |
| TC_APV_009 | 결재문서 상세 조회 | 성공 ✅ | 문서 정보 정확히 출력됨 |

[GitHub Repository - Approval Service](https://github.com/ehgus8/petwiz-back/tree/main/approval-service)
