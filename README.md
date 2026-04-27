# sleepCare 🌙
**수면환경 적응형 스마트 알람 서비스**

수면 데이터와 웨어러블 기기 분석을 통해 최적의 기상 타이밍을 찾아주는 스마트 알람 애플리케이션입니다.

---

## 🚀 Quick Start

### 한 번에 실행 (권장)

저장소 루트에서 한 줄이면 백엔드(:9000) + 프론트엔드(:5173)가 동시에 뜹니다.

```bash
npm install         # 처음 한 번 — concurrently + frontend 의존성 설치
npm run dev
```

출력은 `[backend]` / `[frontend]` 라벨이 붙은 채 한 화면에 보이고, `Ctrl+C` 한 번이면 둘 다 종료됩니다.

> Windows의 PowerShell / cmd / Git Bash, macOS / Linux 어디서 실행해도 동일하게 동작합니다 (특정 셸 의존 없음 — `scripts/start-backend.js`가 OS별로 알맞은 gradle 래퍼를 호출).

> Fitbit 등 새 데이터 소스를 붙여도 이 명령은 그대로입니다 — 추가는 백엔드 코드 안에서 일어나고, 띄우는 방식은 변하지 않습니다.

### 개별 실행

문제 추적 등으로 따로 띄우고 싶을 때:

```bash
# 백엔드만
cd spring-server && ./gradlew bootRun
# 프론트만
cd frontend && npm run dev
```

- 백엔드: `http://localhost:9000`  /  H2 콘솔: `http://localhost:9000/h2-console` (JDBC `jdbc:h2:mem:testdb`, User `sa`, 비밀번호 비움)
- 프론트: `http://localhost:5173` — `/api/*` 호출은 [Vite 프록시](frontend/vite.config.ts)가 :9000으로 전달
- 다른 프로파일로 백엔드 띄우기: `./gradlew bootRun -Pprofile=prod`
- 인메모리 H2 + 개발용 JWT 키가 기본으로 잡혀 있어 별도 환경변수 설정은 불필요합니다.

> **포트 충돌 시**
> ```powershell
> Stop-Process -Id (Get-NetTCPConnection -LocalPort 9000).OwningProcess -Force
> Stop-Process -Id (Get-NetTCPConnection -LocalPort 5173).OwningProcess -Force
> ```

---

## 🖱 사용해보기 (UI 동작 확인)

`npm run dev`로 두 서버가 떴다면, 브라우저에서 **`http://localhost:5173`** 접속.

> 💡 H2는 **인메모리** DB라 백엔드를 재시작하면 가입한 계정·설정한 알람이 모두 초기화됩니다. UI 테스트할 때 매번 회원가입부터 시작하면 됩니다.

| # | 화면 | 입력 | 기대 동작 |
|---|------|------|-----------|
| 1 | 로그인 화면 | — | "회원가입" 버튼 클릭 |
| 2 | 회원가입 | 아이디 `test123` / 비밀번호 `testpass1` / 비밀번호 확인 동일 | "계정 생성하기" → "회원가입이 완료되었습니다." 알림 후 로그인 화면으로 |
| 3 | 로그인 | 같은 아이디/비밀번호 | "로그인" → 대시보드 진입 |
| 4 | 대시보드 | — | 수면 효율 0%, 평균 수면 0h 0m, 가이드 메시지가 표시됨 *(수면 데이터 없을 때 정상값)* |
| 5 | 알람 카드 클릭 | 시간 입력 (예: 07:30) | "알람 저장하기" → 대시보드 복귀, 카드에 07:30 표시 |
| 6 | 로그아웃 | — | 토큰 삭제 + 로그인 화면 |

### 잘못된 입력으로 검증 확인
- **아이디 4자**(`abcd`) → "userId는 영문 소문자와 숫자로 5~20자여야 합니다."
- **대문자 포함**(`USER123`) → 같은 메시지
- **이미 가입된 아이디** → "이미 존재하는 아이디입니다."
- **잘못된 비밀번호로 로그인** → "아이디 또는 비밀번호가 일치하지 않습니다."

알림은 화면 하단에 토스트로 3초간 표시됩니다.

### 백엔드 데이터 직접 확인
H2 콘솔(`http://localhost:9000/h2-console`)에 접속해서 가입한 계정과 알람을 SQL로 확인할 수 있습니다.
```sql
SELECT * FROM users;
SELECT * FROM alarms;
```

---

## 🌐 외부 접속 (포트포워딩 / 다른 기기에서 접속)

기본값은 모두 `localhost`로 동작합니다. 다른 기기에서 접속하거나 백엔드가 외부 URL로 노출됐을 때만 아래 설정이 필요합니다.

### 시나리오 A — 백엔드만 외부 URL로 열린 경우 *(가장 흔함)*
프론트는 여전히 내 노트북에서 띄우고, 백엔드가 `http://203.0.113.10:9000` 같은 공인 주소로 열렸을 때.

1. `frontend/.env.example`을 `frontend/.env`로 복사:
   ```bash
   cp frontend/.env.example frontend/.env
   ```
2. `frontend/.env`에서 받은 주소로 한 줄만 수정:
   ```
   VITE_API_TARGET=http://203.0.113.10:9000
   ```
3. 프론트만 재시작 (`npm run dev`).

[Vite 프록시](frontend/vite.config.ts)가 자동으로 그 주소를 백엔드로 인식해 `/api/*`를 전달합니다. 프론트 코드 자체는 한 줄도 안 고쳐도 됩니다.

### 시나리오 B — 휴대폰/다른 기기에서 같은 WiFi의 내 노트북에 접속
백엔드와 프론트 모두 내 노트북에서 띄우고, 휴대폰으로 화면을 열어보고 싶을 때.

1. 노트북에서 Vite를 외부 노출 모드로 띄우기:
   ```bash
   cd frontend && npm run dev -- --host
   ```
   (또는 [`frontend/vite.config.ts`](frontend/vite.config.ts)의 `server`에 `host: true` 추가)
2. 노트북 IP 확인 (PowerShell): `Get-NetIPAddress -AddressFamily IPv4 | Select InterfaceAlias, IPAddress`
3. 휴대폰 브라우저에서 `http://<노트북-IP>:5173` 접속.
4. 방화벽이 5173/9000 포트를 막고 있으면 인바운드 규칙 추가 필요.

### 시나리오 C — 프론트도 어딘가에 배포한 경우
프론트가 별도 호스팅에서 서비스되고 백엔드가 다른 도메인이면, 호스팅 설정에서 `/api/*`를 백엔드로 reverse proxy 하거나 (Nginx · Cloudflare 등), Spring Boot에 CORS 설정을 추가해야 합니다. 본 저장소엔 아직 CORS 설정이 없으므로 배포 단계에 따로 작업합니다.

---

## 📡 API

| Method | Path | 인증 | 설명 |
|---|---|:---:|---|
| POST | `/api/auth/signup` |   | 회원가입 |
| POST | `/api/auth/login`  |   | 로그인 → JWT 발급 |
| GET  | `/api/alarms`      | ✓ | 알람 조회 (미설정 시 `00:00 / false`) |
| POST | `/api/alarms`      | ✓ | 알람 시간 저장 (upsert) |
| GET  | `/api/dashboard`   | ✓ | 수면 효율 · 평균 수면 시간 · 가이드 메시지 |

- 인증이 필요한 엔드포인트는 `Authorization: Bearer <token>` 헤더 필수.
- 입력 검증: `userId`/`password`는 영문 소문자+숫자, 각각 **5~20자** / **8~20자**.
- 에러 응답 포맷: `{ "errorCode": "AUTH_001", "message": "...", "timestamp": "..." }`
- 상세 스펙은 [api_specification.md](api_specification.md) 참고.

---

## 🛠 Tech Stack

| 구분 | 기술 |
|---|---|
| Frontend | React 18 + Vite + TypeScript + Tailwind CSS |
| Backend  | Spring Boot 3.0 (Java 17, JDK 21 호환) + Spring JDBC + Spring Security |
| Auth     | JWT (jjwt 0.11.2, HS256) |
| DB       | H2 (개발) / MySQL (운영) |
| Validation | Jakarta Bean Validation |
| Build    | Gradle 8.9 |

---

## 📂 Project Structure

```
sleepCare/
├── package.json                        # 루트: `npm run dev`로 두 서버 동시 실행
├── scripts/
│   └── start-backend.js                # OS별 gradle 래퍼를 직접 호출 (셸 의존 없음)
├── frontend/                           # React 클라이언트
│   ├── src/
│   │   ├── api/                        # 백엔드 호출 모듈 (auth, alarm, dashboard)
│   │   ├── components/                 # Button, InputField, AlarmCard …
│   │   ├── layouts/                    # PageWrapper
│   │   ├── pages/                      # Login / Signup / Home / SetAlarm
│   │   └── hooks/                      # useNotification
│   └── vite.config.ts                  # /api → :9000 프록시
├── spring-server/                      # Spring Boot 백엔드
│   └── src/main/java/project/server/
│       ├── controller/api/             # AuthApi / Alarm / Dashboard
│       ├── service/api/
│       ├── dao/                        # AccountDao / AlarmDao / SleepRecordDao
│       ├── dto/api/                    # 요청·응답 DTO
│       ├── common/exception/api/       # ApiException + ErrorCode
│       ├── common/interceptor/         # ApiJwtInterceptor
│       └── util/jwt/                   # JwtTokenProvider
├── docs/
│   └── fitbit-integration-guide.md    # Fitbit 데이터 연동 가이드
├── api_specification.md
└── README.md
```

> 기존 `/users/**`, `/auth/login` 엔드포인트와 `user` (단수) 테이블은 레거시로 그대로 유지되며, PDF 기술서 기반 신규 도메인은 `/api/**` 와 `users` (복수) 테이블에 분리되어 있습니다.

---

## 🗄 데이터베이스 (PDF 기술서 기준)

| 테이블 | 용도 |
|---|---|
| `users` | 회원 (id, username, password, created_at, updated_at) |
| `alarms` | 사용자별 알람 (target_time, is_active) |
| `sleep_records` | 수면 기록 (sleep_start/end, total_sleep_minutes, sleep_score) |
| `sleep_environments` | 라즈베리파이/센서 환경 데이터 (temperature, humidity, co2, light, noise) |
| `alarm_adjustments` | 적응형 알람 알고리즘 산출물 (recommended_time, reason …) |

> 현재 `/api/dashboard`는 `sleep_records`를 7일치 집계합니다. 데이터 입력 채널(웨어러블 / 라즈베리파이)이 아직 없어 초기 응답은 `0% / 0h 0m`입니다. 연동 방법은 [docs/fitbit-integration-guide.md](docs/fitbit-integration-guide.md) 참고.

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
