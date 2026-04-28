# SleepCare API 명세서

본 문서는 SleepCare 프런트엔드와 Spring Boot 백엔드 간의 데이터 통신을 위한 최신 API 규격입니다. Swagger UI를 기반으로 재작성되었으며, 모든 응답은 공통 래퍼 포맷으로 제공됩니다.

## 공통 응답 포맷 (Wrapper)
모든 정상(200 OK) 응답은 아래와 같은 규격으로 감싸져 반환되며, 실제 데이터는 `result` 필드에 포함됩니다.
```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다.",
  "result": { ... 실제 데이터 객체 ... }
}
```

에러 발생 시(400, 401, 500 등) 응답 포맷:
```json
{
  "code": 4004,
  "status": 400,
  "message": "비밀번호가 일치하지 않습니다.",
  "timestamp": "2026-04-28T15:00:00Z"
}
```

---

## 1. 회원 및 인증 API (User & Auth)

### [POST] /users
회원가입을 진행하고 성공 즉시 JWT를 반환합니다.

**Request Body:**
```json
{
  "nickname": "sleepy_user",
  "password": "Password123!"
}
```

**Response (200 OK) - `result` 필드 내용:**
```json
{
  "userId": 1,
  "jwt": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### [GET] /users/me
현재 로그인한 사용자의 프로필(닉네임)을 조회합니다. (헤더에 Bearer 토큰 필요)

**Response (200 OK) - `result` 필드 내용:**
```json
{
  "nickname": "sleepy_user"
}
```

---

### [POST] /auth/login
로그인을 진행하고 JWT 토큰을 발급받습니다.

**Request Body:**
```json
{
  "nickname": "sleepy_user",
  "password": "Password123!"
}
```

**Response (200 OK) - `result` 필드 내용:**
```json
{
  "userId": 1,
  "jwt": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## 2. 알람 API (Alarm)

### [GET] /alarms
현재 사용자의 요일별 알람 설정 및 오늘 기상해야 할 동적 알람 시간을 조회합니다. (헤더에 Bearer 토큰 필요)

**Response (200 OK) - `result` 필드 내용:**
```json
{
  "todayDayOfWeek": 2,
  "todayEffectiveWakeAt": "2026-04-29T22:15:00Z",
  "alarms": [
    {
      "dayOfWeek": 2,
      "baseWakeTime": "07:30",
      "dynamicWakeAt": "2026-04-29T22:15:00Z",
      "adaptiveEnabled": true,
      "windowMinutesBefore": 30
    }
  ]
}
```

---

### [PATCH] /alarms
알람 설정(시간 및 적응형 모드)을 업데이트(upsert)합니다. (헤더에 Bearer 토큰 필요)

**Request Body:**
```json
{
  "dayOfWeek": 2,
  "baseWakeTime": "07:30",
  "adaptiveEnabled": true,
  "windowMinutesBefore": 30
}
```

**Response (200 OK) - `result` 필드 내용:**
(GET /alarms 와 동일한 최신 알람 상태 객체 반환)

---

## 3. 대시보드 API (Dashboard)

### [GET] /dashboard/sleep-summary
홈 화면에 표시할 수면 효율, 평균 수면 시간, 수면 가이드 메시지 요약 데이터를 조회합니다. (헤더에 Bearer 토큰 필요)

**Response (200 OK) - `result` 필드 내용:**
```json
{
  "sleepEfficiencyPercent": 92,
  "averageSleepDurationMinutes": 440,
  "environmentHint": "환경 신호가 안정적입니다. 같은 조건으로 수면 루틴을 유지해 보세요."
}
```
