# sleepCare 🌙
**수면환경 적응형 스마트 알람 서비스**

수면 데이터와 웨어러블 기기 분석을 통해 최적의 기상 타이밍을 찾아주는 스마트 알람 애플리케이션입니다.

---

## 🚀 Quick Start

### 한 번에 실행 (권장)

저장소 루트에서 한 줄이면 백엔드(:9000) + 프론트엔드(:5173)가 동시에 뜹니다.

```bash
npm run install:all   # 처음 한 번 — 루트 concurrently + frontend 의존성
npm install           # 이후 루트 의존성만 갱신할 때
npm run dev
```

출력은 `[backend]` / `[frontend]` 라벨이 붙은 채 한 화면에 보이고, `Ctrl+C` 한 번이면 둘 다 종료됩니다.

> Windows의 PowerShell / cmd / Git Bash, macOS / Linux 어디서 실행해도 동일하게 동작합니다 (`scripts/run-backend.cjs`가 `spring-server` 안의 Gradle 래퍼를 호출합니다. 터미널에서 `cd` 할 필요 없음).

### 개별 실행 (루트에서)

```bash
npm run dev:backend    # spring-server Gradle bootRun (프로파일: SPRING_PROFILE 또는 기본 local-h2)
npm run dev:frontend   # Vite — `frontend/.env` 의 VITE_API_BASE_URL 사용
```

루트에서 Gradle을 직접 쓰고 싶다면(선택):

```bash
node scripts/run-gradle.cjs bootRun -Pprofile=local-h2
node scripts/run-gradle.cjs build
```

- 백엔드: `http://localhost:9000`  /  H2 콘솔: `http://localhost:9000/h2-console` (JDBC `jdbc:h2:file:./data/sleepcare`, User `sa`, 비밀번호 비움)
- 프론트: `http://localhost:5173`
- 다른 프로파일로 백엔드: `SPRING_PROFILE=prod npm run dev:backend` (Windows PowerShell: `$env:SPRING_PROFILE='prod'; npm run dev:backend`)

> **포트 충돌 시**
> ```powershell
> Stop-Process -Id (Get-NetTCPConnection -LocalPort 9000).OwningProcess -Force
> Stop-Process -Id (Get-NetTCPConnection -LocalPort 5173).OwningProcess -Force
> ```

---

## 🌐 환경 변수 (루트 단일 `.env`)

백엔드와 프론트가 **저장소 루트**의 `.env`를 공유합니다. 템플릿은 `.env.sample`을 복사해 `.env`를 만든 뒤 값을 채우면 됩니다.

- `VITE_API_BASE_URL`: 백엔드 베이스 URL (로컬 기본 `http://localhost:9000`) — `frontend/.env`에 설정
- DB·JWT 등은 `spring-server`의 `application.yml`이 참조하는 변수명과 동일하게 설정합니다.

값을 바꾼 뒤에는 해당 프로세스를 재시작하면 됩니다.

### 같은 WiFi의 다른 기기에서 접속

```bash
npm run dev:frontend -- --host
```

노트북 IP 확인(PowerShell): `Get-NetIPAddress -AddressFamily IPv4 | Select InterfaceAlias, IPAddress`
→ 휴대폰 브라우저에서 `http://<노트북-IP>:5173` 접속.

---

## 🖱 사용해보기 (UI 동작 확인)

`npm run dev` 후 브라우저에서 **`http://localhost:5173`** 접속.

| # | 화면 | 입력 | 기대 동작 |
|---|------|------|-----------|
| 1 | 로그인 화면 | — | "회원가입" 버튼 클릭 |
| 2 | 회원가입 | 닉네임 `nickname` / 비밀번호 `password` / (선택) Fitbit 아이디·비밀번호 | "계정 생성하기" → "회원가입이 완료되었습니다." 알림 후 대시보드 바로 진입 |
| 3 | 로그인 | 같은 닉네임/비밀번호 | "로그인" → 대시보드 진입 |
| 4 | 대시보드 | — | 수면 효율·평균 수면 시간·가이드 메시지 표시 *(데이터 없을 때 `0% / 0h 0m` 정상)* |
| 5 | 알람 카드 클릭 | 요일별(월~일) 7행에서 시간·적응형 모드·윈도우 설정 후 저장 | 대시보드 복귀, 오늘 기상 시간 카드에 반영 |
| 6 | 로그아웃 | — | 토큰 삭제 + 로그인 화면 |

### 잘못된 입력으로 검증 확인
- **닉네임 3자** → 4자 이상 입력 안내
- **허용되지 않은 문자**(`user!@#`) → 영문/숫자/_ 안내
- **이미 가입된 닉네임** → "이미 존재하는 아이디입니다."
- **잘못된 비밀번호로 로그인** → "아이디 또는 비밀번호가 일치하지 않습니다."

알림은 화면 하단에 토스트로 3초간 표시됩니다.

### 로컬 백엔드 데이터 직접 확인
H2 콘솔(`http://localhost:9000/h2-console`)에서 SQL로 확인 가능합니다.
```sql
SELECT * FROM users;
SELECT * FROM daily_alarms;
SELECT * FROM sleep_records;
```

---

## 📡 API

| Method | Path | 인증 | 설명 |
|---|---|:---:|---|
| POST  | `/users`                   |   | 회원가입 → JWT 즉시 발급 |
| GET   | `/users/me`                | ✓ | 로그인 사용자 닉네임 조회 |
| POST  | `/auth/login`              |   | 로그인 → JWT 발급 |
| GET   | `/alarms`                  | ✓ | 요일별 알람 목록 및 오늘 기상 시간 조회 |
| PATCH | `/alarms`                  | ✓ | 요일별 알람 시간·적응형 모드 설정 (upsert) |
| GET   | `/dashboard/sleep-summary` | ✓ | 수면 효율 · 평균 수면 시간(분) · 가이드 메시지 |

- 인증이 필요한 엔드포인트는 `Authorization: Bearer <token>` 헤더 필수.
- `nickname`: 영문/숫자/_ 조합 4~20자 / `password`: 8~30자
- 원격 백엔드는 `{ "code": 1000, "status": 200, "message": "...", "result": { ... } }` 래퍼 포맷 사용. 로컬 백엔드는 `result` 객체를 직접 반환 (프론트 클라이언트가 양쪽 모두 처리).
- 상세 스펙은 [docs/api_specification.md](docs/api_specification.md) 참고.

---

## 🛠 Tech Stack

| 구분 | 기술 |
|---|---|
| Frontend | React 18 + Vite + TypeScript + Tailwind CSS |
| Backend  | Spring Boot 3.0 (Java 17, JDK 21 호환) + Spring JDBC + Spring Security |
| Auth     | JWT (jjwt 0.11.2, HS256) |
| DB       | H2 file-based (개발) / MySQL (운영) |
| Crypto   | AES-256-GCM (Fitbit 비밀번호 암호화) |
| Validation | Jakarta Bean Validation |
| Build    | Gradle 8.9 |

---

## 📂 Project Structure

```
sleepCare/
├── .env.sample                         # 루트 `.env` 템플릿 (백엔드 + VITE_*)
├── package.json                        # 루트: `npm run dev` 등 (하위 폴더로 cd 불필요)
├── scripts/
│   ├── run-backend.cjs                 # `bootRun` (루트에서 실행)
│   └── run-gradle.cjs                  # 기타 Gradle 태스크 (`build`, `test` …)
├── frontend/                           # React 클라이언트
│   ├── src/
│   │   ├── api/                        # auth, alarm, dashboard, user (클라이언트 모듈)
│   │   ├── components/                 # Button, InputField, AlarmCard …
│   │   ├── layouts/                    # PageWrapper
│   │   ├── pages/                      # Login / Signup / Home / SetAlarm (요일별 7행)
│   │   └── hooks/                      # useNotification
│   └── vite.config.ts
├── spring-server/                      # Spring Boot 백엔드
│   └── src/main/java/project/server/
│       ├── controller/api/             # UserController / AuthController / AlarmController / DashboardController
│       ├── service/api/                # AccountAuthService / AlarmService / DashboardService
│       ├── dao/                        # AccountDao / AlarmDao (daily_alarms) / SleepRecordDao
│       ├── dto/api/                    # 요청·응답 DTO
│       ├── common/exception/api/       # ApiException + ApiErrorCode
│       ├── common/interceptor/         # ApiJwtInterceptor
│       ├── util/jwt/                   # JwtTokenProvider
│       └── util/crypto/               # AesGcmCipher (Fitbit 비밀번호 암호화)
├── docs/
│   ├── api_specification.md            # API 통신 명세서
│   └── fitbit-integration-guide.md    # Fitbit 수면 데이터 연동 가이드
└── README.md
```

---

## 🗄 데이터베이스

| 테이블 | 용도 |
|---|---|
| `users` | 회원 (id, nickname, password, fitbit_user_id, fitbit_user_password_enc) |
| `daily_alarms` | 요일별 알람 (user_id, day_of_week, base_wake_time, adaptive_enabled, window_minutes_before, dynamic_wake_at) |
| `sleep_records` | 수면 기록 (sleep_start/end, total_sleep_minutes, sleep_score) |
| `sleep_environments` | 센서 환경 데이터 (temperature, humidity, co2, light, noise) |
| `alarm_adjustments` | 적응형 알람 산출물 (recommended_time, reason …) |

> `/dashboard/sleep-summary`는 `sleep_records`를 7일치 집계합니다. 웨어러블/라즈베리파이 연동 전까지 초기값은 `0% / 0분`입니다. 연동 방법은 [docs/fitbit-integration-guide.md](docs/fitbit-integration-guide.md) 참고.

---

## 📝 Git Commit Convention

```
<Type>: <설명>
```

|Type|설명|
|---|---|
|**Feat**|새로운 기능 추가|
|**Fix**|버그 수정|
|**Refactor**|리팩토링|
|**Design**|UI 변경|
|**Comment**|주석|
|**Style**|포맷팅 (로직 변경 X)|
|**Test**|테스트 코드|
|**Chore**|빌드 / 패키지 / 기타|
|**Init**|초기 생성|
|**Rename**|파일·폴더 이동|
|**Remove**|파일 삭제|
