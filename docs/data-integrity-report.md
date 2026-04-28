# SleepCare 데이터 정합성 검증 보고서

> ⚠️ **Disclaimer (2026-04-28 업데이트)** 
> 본 보고서는 프로젝트 초기(인메모리 DB 및 `userId` 기반)의 테스트 기록입니다. 
> 현재 시스템은 **파일 기반 H2 영구 DB(`data/sleepcare.mv.db`)**와 **`nickname` 기반 식별자**, 그리고 **스웨거 기준의 API 경로(`/users`, `/auth/login` 등)**로 진화하였으므로, 아래의 테스트 결과는 과거 기록의 참고용으로만 활용하시기 바랍니다.

> 검증일: 2026-04-27 16:12 ~ 16:23 KST | 환경: H2 인메모리 DB (local-h2 프로파일)

---

## 1. 테스트 시나리오 및 결과

### 1-1. 회원가입 (3명)

| # | userId | password | HTTP | 결과 |
|---|---|---|:---:|---|
| 1 | `testuser01` | `testpass1` | **201** | ✅ 가입 성공 |
| 2 | `testuser02` | `securepass2` | **201** | ✅ 가입 성공 |
| 3 | `testuser03` | `mypassword3` | **201** | ✅ 가입 성공 |

### 1-2. 중복 가입 차단

| 시도 | HTTP | errorCode | 메시지 |
|---|:---:|---|---|
| `testuser01` 재가입 | **409** | `AUTH_002` | 이미 존재하는 아이디입니다 |

✅ **정상 차단** — 동일 username 중복 INSERT 방지됨

### 1-3. 로그인 + JWT 발급

| userId | HTTP | 토큰 subject | userName |
|---|:---:|---|---|
| `testuser01` | **200** | `1` (users.id) | testuser01 |
| `testuser02` | **200** | `2` (users.id) | testuser02 |
| `testuser03` | **200** | `3` (users.id) | testuser03 |

✅ **JWT subject에 users.id가 정확히 들어감** — ApiJwtInterceptor가 Long.parseLong으로 추출 가능

### 1-4. 알람 설정 + 수정 (Upsert)

| 사용자 | 동작 | 입력 | 응답 alarmTime | DB target_time |
|---|---|---|---|---|
| user01 | 최초 설정 | `07:30` | `07:30` | `07:30:00` |
| user01 | **수정** | `06:15` | `06:15` | `06:15:00` |
| user02 | 최초 설정 | `23:45` | `23:45` | `23:45:00` |

✅ **Upsert 동작 정상** — 처음엔 INSERT, 두 번째는 UPDATE (alarms 테이블에 row 1개만 유지)

### 1-5. 대시보드 조회

| 사용자 | sleepEfficiency | averageSleepTime | guideMessage |
|---|:---:|---|---|
| user01 | `0` | `0h 0m` | "아직 수면 데이터가 없습니다..." |
| user02 | `0` | `0h 0m` | "아직 수면 데이터가 없습니다..." |

✅ **정상** — sleep_records에 데이터가 없으므로 0% / 0h 0m + 동적 안내 메시지 출력

---

## 2. H2 DB 직접 확인

### 2-1. `users` 테이블

![H2 users 테이블 조회 결과](C:/Users/jhsoo/.gemini/antigravity/brain/06994f50-6872-4d4e-8971-9bd28a912c4f/artifacts/h2_users_table.png)

| id | username | password (prefix) | created_at |
|:---:|---|---|---|
| 1 | testuser01 | `$2a$10$puibpfiu...` | 2026-04-27 16:13:19 |
| 2 | testuser02 | `$2a$10$M033/OQX...` | 2026-04-27 16:14:52 |
| 3 | testuser03 | `$2a$10$gW0IK8pl...` | 2026-04-27 16:14:55 |

✅ **확인 사항:**
- **AUTO_INCREMENT** 정상 (1, 2, 3 순차)
- **BCrypt 해시** 정상 (`$2a$10$` 프리픽스 — BCrypt 10라운드)
- **평문 비밀번호 저장 안 됨** — 보안 정합성 확보
- **created_at** 자동 설정 정상

### 2-2. `alarms` 테이블

![H2 alarms 테이블 조회 결과](C:/Users/jhsoo/.gemini/antigravity/brain/06994f50-6872-4d4e-8971-9bd28a912c4f/artifacts/h2_alarms_table.png)

| id | user_id | username | target_time | is_active | created_at | updated_at |
|:---:|:---:|---|---|:---:|---|---|
| 1 | 1 | testuser01 | 06:15:00 | TRUE | 2026-04-27 16:18:07 | 2026-04-27 16:18:07 |
| 2 | 2 | testuser02 | 23:45:00 | TRUE | 2026-04-27 16:18:22 | 2026-04-27 16:18:22 |

✅ **확인 사항:**
- **외래키 관계** 정상 — `alarms.user_id` → `users.id` 정확히 매핑
- **user01 알람 = 06:15** — 07:30에서 06:15로 수정됨 (upsert 정상)
- **user03 알람 없음** — 미설정 사용자에게 row가 생기지 않음
- **is_active = TRUE** — 알람 설정 시 자동 활성화

### 2-3. 빈 테이블 확인

| 테이블 | 행 수 | 예상 | 결과 |
|---|:---:|:---:|:---:|
| `sleep_records` | 0 | 0 | ✅ |
| `sleep_environments` | 0 | 0 | ✅ |
| `alarm_adjustments` | 0 | 0 | ✅ |

---

## 3. 엣지 케이스 검증

### 3-1. 입력 검증 (Validation)

| 테스트 | 입력 | HTTP | errorCode | 메시지 |
|---|---|:---:|---|---|
| 짧은 userId (4자) | `abcd` | **400** | `VALIDATION_001` | userId는 영문 소문자와 숫자로 5~20자 |
| 대문자 userId | `USER1234` | **400** | `VALIDATION_001` | userId는 영문 소문자와 숫자로 5~20자 |
| 짧은 password (5자) | `short` | **400** | `VALIDATION_001` | password는 영문 소문자와 숫자로 8~20자 |

✅ **모든 Validation 정상 동작** — `@Pattern` 어노테이션이 정확히 작동

### 3-2. 인증 실패

| 테스트 | HTTP | errorCode | 메시지 |
|---|:---:|---|---|
| 토큰 없이 `/api/alarms` 접근 | **401** | `AUTH_003` | 인증이 필요합니다 |
| 잘못된 비밀번호 로그인 | **401** | `AUTH_001` | 아이디 또는 비밀번호가 일치하지 않습니다 |
| 존재하지 않는 사용자 로그인 | **401** | `AUTH_001` | 아이디 또는 비밀번호가 일치하지 않습니다 |

✅ **보안 정합성** — 사용자 존재 여부를 노출하지 않음 (동일한 AUTH_001 메시지)

### 3-3. 사용자 간 데이터 격리 (Cross-User Isolation)

| 항목 | user01 | user02 | user03 |
|---|---|---|---|
| 알람 시간 | `06:15` | `23:45` | `00:00` (미설정) |
| 알람 활성 | `true` | `true` | `false` |
| 대시보드 | 독립 집계 | 독립 집계 | 독립 집계 |

✅ **완전 격리 확인** — 각 사용자의 JWT 토큰으로 조회 시 자기 데이터만 반환

---

## 4. 데이터 정합성 종합 평가

| 검증 항목 | 결과 | 비고 |
|---|:---:|---|
| 회원가입 → DB 저장 | ✅ | username, BCrypt 해시 정상 |
| 중복 가입 방지 | ✅ | UNIQUE 제약 + 서비스 레벨 체크 |
| 로그인 → JWT 발급 | ✅ | subject=users.id, 올바른 서명 |
| JWT → userId 추출 | ✅ | Interceptor → ArgumentResolver 연동 |
| 알람 CRUD | ✅ | INSERT/UPDATE(upsert) + 외래키 |
| 대시보드 집계 | ✅ | COALESCE로 NULL 안전 처리 |
| 입력 검증 | ✅ | 패턴, 길이 제약 모두 동작 |
| 에러 응답 일관성 | ✅ | errorCode + message + timestamp 포맷 |
| 보안 (비밀번호) | ✅ | BCrypt 해시 저장, 평문 미노출 |
| 보안 (토큰) | ✅ | 미인증 요청 401 차단 |
| 사용자 격리 | ✅ | 다른 사용자 데이터 접근 불가 |

> [!TIP]
> **결론: 데이터 정합성에 문제가 없습니다.** 회원가입부터 알람 설정까지 전체 흐름에서 DB에 저장되는 데이터가 API 응답과 일관되게 유지됩니다. BCrypt 해시, 외래키 관계, 자동 타임스탬프, 사용자 격리 모두 정상입니다.
