# 적응형 기상 알고리즘 (Adaptive Wake-up Algorithm)

## 왜 이 알고리즘이 필요한가

고정된 시간에 알람을 맞추면 **깊은 수면(Deep Sleep) 도중 강제 기상**이 일어날 수 있다.
깊은 수면에서 갑자기 깨어나면 *수면 관성(Sleep Inertia)* — 심각한 인지 저하·극심한 피로감 — 이 최대 수십 분간 지속된다.

이 알고리즘은 사용자가 설정한 목표 기상 시각 **직전의 탐색 창(window)** 안에서
가장 얕은 수면(light·REM) 구간을 찾아 그 시점에 알람을 울린다.
자연스러운 수면 주기 상에서 깨어나도록 유도해 수면 관성을 최소화하는 것이 목적이다.

---

## 데이터 흐름

```
Fitbit 워치  ──Fitbit API──►  fitbit_to_rds Lambda  ──INSERT──►  RDS
                                                                    │
                                                             sleep_stage
                                                       daily_health_summary
                                                                    │
                                              ◄──────  DynamicAlarmService
                                    알람 재계산 (기상 1시간 전 자동 스케줄러 OR GET /alarms 호출 시)
```

- **`sleep_stage`**: 수면 구간별 단계 타임라인 (deep / light / rem / wake)
- **`daily_health_summary`**: 일별 수면 효율·단계 비율 요약 (점수 계산에 사용)
- **`record_date`**: Fitbit이 **취침 시작일** 기준으로 기록 → 아침 7시 기상이면 어제 날짜

---

## 전체 처리 흐름 (DynamicAlarmService.recalculateForUser)

```
입력: userId
         │
         ▼
1. 오늘 요일의 alarm 행 조회
   없음 → 종료 (변경 없음)
         │
         ▼
2. adaptive_enabled = false?
   YES → dynamic_wake_at = nearestUpcomingWakeAt(baseWakeTime) → 종료
         │ NO
         ▼
3. windowEnd = 오늘(KST) + baseWakeTime
   이미 지났나? (now ≥ dynamicWakeAt  OR  now ≥ windowEnd)
   YES → dynamic_wake_at = nextWeeklyWakeAt (다음 주 동일 요일) → 종료
         │ NO
         ▼
4. 최신 SleepStage 조회 및 시간 맵핑 (Shift)
   실시간(오늘) 수면 데이터가 없으면 가장 최신 기록을 불러와
   해당 수면 시간대를 오늘 날짜 기준으로 맵핑(Shift)하여 패턴 유추
         │
         ▼
5. 수면 점수 계산 → effectiveWindow 결정
   daily_health_summary(최근 7일) 있음 → 가중 평균 점수
   없음, SleepStage 있음 → 간이 점수
   둘 다 없음 → 점수 100 (창 조정 없음)
         │
         ▼
6. windowStart = windowEnd - effectiveWindow
         │
         ▼
7. 맵핑된(Shift) 각 SleepStage 세그먼트에 대해:
   deep 포함 → 제외
   light/rem/restless/awake/wake 포함
     + [맵핑된 segStart, 맵핑된 segEnd] ∩ [windowStart, windowEnd] ≠ ∅
   → 후보 = max(맵핑된 segStart, windowStart)
         │
         ▼
8. 후보 중 now 이상인 최솟값 선택
   없으면 → windowEnd (Fallback)
         │
         ▼
9. dynamic_wake_at 저장 + MQTT 스케줄 발행 (라즈베리파이 전송)
```

---

## 핵심 개념별 상세 설명

### 탐색 창 (Window)

```
─────────────────────────────────────────────────────────── 시간 축
              │              │              │
          windowStart    (탐색 구간)    windowEnd
              │◄──── effectiveWindow ────►│
              │                           │
              │                      = base_wake_time
              │                        (07:30)
         base_wake_time - effectiveWindow
              (예: 07:00 ~ 07:30, 30분 창)
```

- `windowEnd` = 오늘 날짜 + `base_wake_time` (예: `2026-05-31T07:30`)
- `windowStart` = `windowEnd - effectiveWindow` (예: `2026-05-31T07:00`)
- `effectiveWindow` = 사용자 설정값 + 수면 점수 기반 추가 분

### 후보 시각 계산 (`max(segStart, windowStart)`)

수면 세그먼트가 창 앞에서 시작했더라도, 창 **안에 걸치면** 창 시작점(windowStart)을 후보로 취한다.

```
예시: segStart=06:50, segEnd=07:15, windowStart=07:00, windowEnd=07:30

  06:50        07:00        07:15        07:30
    │─── light ──┼────────────┼            │
                 │◄──window──────────────►│
                 │
                 └─ 후보 = max(06:50, 07:00) = 07:00
```

### 최신 기록 기반 시간 맵핑 (Shift)

Fitbit 데이터는 아침 기상 후에 일괄 업데이트 되는 특성이 있어, 기상 1시간 전 스케줄러가 동작할 시점에는 오늘 밤의 실시간 수면 데이터가 없을 가능성이 높습니다.
이 한계를 극복하기 위해, **가장 최근의 수면 기록(예: 어젯밤)을 불러와 그 시간대를 오늘 날짜에 맞게 Shift** 합니다.

```
예시: 오늘(6월 10일) 기상 1시간 전(06:30) 알고리즘 자동 실행
가장 최신 기록: 6월 8일의 수면 데이터 (6월 9일 기상 시 적재됨)

[과거 기록] 6월 8일 23:00 ~ 6월 9일 07:30 수면
  → 이 과거의 수면 패턴을 오늘(6월 10일)로 맵핑(Shift: +1일)
  → [맵핑 후] 6월 9일 23:00 ~ 6월 10일 07:30 수면으로 간주하고,
    오늘의 탐색 창(07:00~07:30) 내에서 얕은 수면 구간을 예측해냅니다.
```

---

## 구체적인 시나리오 예시

### 시나리오 A: 정상 작동 (얕은 수면 탐지)

**설정**: `base_wake_time=07:30`, `window_minutes_before=30`, `adaptive=true`
**현재 시각**: 07:05

**어젯밤 수면 타임라인** (`record_date = 2026-05-30`):

| 시작 시각 | 단계 | 지속 |
|---|---|---|
| 23:00 | deep | 90분 |
| 00:30 | light | 45분 |
| 01:15 | rem | 30분 |
| 01:45 | deep | 60분 |
| ... | ... | ... |
| 06:40 | deep | 25분 |
| **07:05** | **light** | **40분** |

**계산 과정**:
```
windowStart = 07:00,  windowEnd = 07:30
now = 07:05

세그먼트 순회:
  07:05 light (07:05~07:45)
    → light 포함 ✅
    → [07:05, 07:45] ∩ [07:00, 07:30] = [07:05, 07:30] ≠ ∅ ✅
    → 후보 = max(07:05, 07:00) = 07:05
    → 07:05 ≥ now(07:05) ✅

최솟값 = 07:05
결과: dynamic_wake_at = 2026-05-31T07:05  ← 07:30보다 25분 일찍 기상!
```

---

### 시나리오 B: Fallback (창 전체가 deep)

**설정**: 동일. 창 안이 전부 deep인 최악의 케이스.

```
windowStart = 07:00,  windowEnd = 07:30

07:00 deep (07:00~07:35)
  → deep 포함 → 제외

후보 없음 → windowEnd = 07:30
결과: dynamic_wake_at = 2026-05-31T07:30  ← 원래 설정 시각 그대로
```

> 알람이 울리지 않는 상황을 방지하기 위한 안전장치.

---

### 시나리오 C: adaptive_enabled = false

```
수면 분석 없이 단순히 다음 해당 요일·시각으로 설정.
결과: dynamic_wake_at = 오늘 07:30 (혹은 이미 지났으면 다음 주 동일 요일 07:30)
```

---

### 시나리오 D: 이미 알람 시각이 지남

**현재 시각**: 07:45 (windowEnd=07:30을 이미 지남)

```
hasPassedWakeSchedule = true
→ dynamic_wake_at = 다음 주 동일 요일 07:30
```

---

## 점수 기반 탐색 창 자동 조정 (SleepQualityEvaluator)

수면 질이 누적으로 나쁘면 창을 자동으로 넓혀 더 이른 시각에 얕은 수면을 탐지할 기회를 늘린다.

### 1일치 수면 점수 계산 (0~100)

**데이터 소스**: `daily_health_summary` 테이블

| 요소 | 계산 공식 | 가중치 | 기준 |
|---|---|---|---|
| 수면 효율 | `efficiency` | **50%** | NSF 기준 85% 이상 = 정상 |
| 깊은 수면 비율 | `min(deepMins / minutesAsleep × 500, 100)` | **25%** | 전체의 20% 이상이면 만점(100) |
| REM 비율 | `min(remMins / minutesAsleep × 500, 100)` | **15%** | 전체의 20% 이상이면 만점(100) |
| 각성 패널티 | `max(0, 100 - wakeMins × 2)` | **10%** | 각성 1분당 -2점 |

```
nightScore = efficiency×0.50 + deepScore×0.25 + remScore×0.15 + wakeScore×0.10
             [0~100으로 클램프]
```

**계산 예시**:
```
efficiency = 82,  deepMins = 65,  minutesAsleep = 380,  remMins = 70,  wakeMins = 12

deepScore = min(65/380 × 500, 100) = min(85.5, 100) = 85.5
remScore  = min(70/380 × 500, 100) = min(92.1, 100) = 92.1
wakeScore = max(0, 100 - 12×2)    = max(0, 76)      = 76

nightScore = 82×0.50 + 85.5×0.25 + 92.1×0.15 + 76×0.10
           = 41.0 + 21.4 + 13.8 + 7.6
           = 83.8  →  반올림 = 84점
```

---

### 누적 점수: 최근 7일 가중 평균

최신 수면에 더 높은 가중치를 부여해 최근 변화가 빠르게 반영된다.

| 날짜 | 가중치 |
|---|---|
| D-1 (어제) | 0.30 |
| D-2 | 0.20 |
| D-3 | 0.15 |
| D-4 | 0.12 |
| D-5 | 0.10 |
| D-6 | 0.08 |
| D-7 | 0.05 |
| **합계** | **1.00** |

데이터 없는 날은 해당 가중치를 제외하고 나머지 가중치를 정규화해 재계산.

**7일 누적 예시**:
```
D-1: 84점 (× 0.30) = 25.2
D-2: 71점 (× 0.20) = 14.2
D-3: 없음 → 제외
D-4: 65점 (× 0.12/(0.12+0.10+0.08+0.05) = 0.12/0.35 = 0.343) = 22.3
D-5: 58점 (× 0.10/0.35 = 0.286) = 16.6
D-6: 없음 → 제외
D-7: 77점 (× 0.08/0.35 = 0.229) = 17.6

D-3, D-6 제외, 나머지 가중치를 (0.30+0.20+0.343+0.286+0.229=1.358)... 
※ 실제 코드는 있는 날만 WEIGHTS[i]를 weightSum에 누적하고 마지막에 scoreSum/weightSum 계산

단순 예시 (모든 7일 데이터 있는 경우):
D-1=80, D-2=75, D-3=70, D-4=65, D-5=60, D-6=55, D-7=50

score = 80×0.30 + 75×0.20 + 70×0.15 + 65×0.12 + 60×0.10 + 55×0.08 + 50×0.05
       = 24.0 + 15.0 + 10.5 + 7.8 + 6.0 + 4.4 + 2.5
       = 70.2  →  70점
```

---

### 점수 → 탐색 창 조정

| 누적 점수 | 수면 상태 | 추가 창 |
|---|---|---|
| 80 – 100 | 양호 | +0분 |
| 60 – 79 | 약간 부족 | +15분 |
| 40 – 59 | 수면 부족 | +30분 |
| 0 – 39 | 심한 수면 부족 | +60분 |

```
effectiveWindow = min(window_minutes_before + 추가 창, 120)  // 최대 120분 캡
```

**전체 예시** (`base_wake_time=07:30`, `window_minutes_before=30`):

```
점수 90 (양호)    → effectiveWindow = 30분  → [07:00 ~ 07:30]
점수 70 (약간 부족) → effectiveWindow = 45분  → [06:45 ~ 07:30]
점수 50 (부족)    → effectiveWindow = 60분  → [06:30 ~ 07:30]
점수 30 (심각)    → effectiveWindow = 90분  → [06:00 ~ 07:30]
```

> 수면이 나쁜 날일수록 더 넓은 시간대에서 얕은 수면을 찾는다.
> 결과적으로 그날의 가장 자연스러운 기상 시점에 알람이 울린다.

---

### 데이터 없을 때 폴백

| 상황 | 사용 점수 | 창 조정 |
|---|---|---|
| `daily_health_summary` 있음 | 가중 평균 점수 계산 | 점수에 따라 |
| 없음 + `sleep_stage` 있음 | 간이 점수 (deep 비율로 추정) | 점수에 따라 |
| 둘 다 없음 | 100점 (기본값) | +0분 (조정 없음) |

**간이 점수 (SleepStage만 있을 때)**:
```
score = 70 - deepRatio×40 + lightRatio×15 + remRatio×15

예) 총 수면 6시간 중 deep=2h, light=2h, rem=1h, 나머지=1h
    deepRatio=0.33, lightRatio=0.33, remRatio=0.17

    score = 70 - 0.33×40 + 0.33×15 + 0.17×15
           = 70 - 13.2 + 4.95 + 2.55
           = 64.3  →  64점 (약간 부족 구간 → +15분)
```

---

## 엣지 케이스 정리

| 케이스 | 처리 결과 |
|---|---|
| 최신 수면 기록이 전혀 없음 | 점수=100으로 창 조정 없이 windowEnd로 Fallback 설정 |
| 창 전체가 deep | 후보 없음 → windowEnd (Fallback) |
| 후보가 now보다 과거 | 필터 제거 → 남은 후보 없으면 windowEnd |
| adaptive=false | 수면 분석 없이 nearestUpcomingWakeAt 직접 사용 |
| 알람 시간 이미 지남 | nextWeeklyWakeAt으로 다음 주 동일 요일 설정 |
| 데이터 전혀 없음 | 점수=100, 창 조정 없음, 수면 분석 없이 windowEnd |

---

## 관련 소스 파일

| 파일 | 역할 |
|---|---|
| `service/DynamicAlarmService.java` | 알고리즘 전체 흐름 |
| `util/SleepQualityEvaluator.java` | 수면 점수 계산 + 창 조정 |
| `service/AlarmSchedulerTask.java` | 기상 1시간 전 자동 동작 스케줄러 |
| `util/AlarmWakeAtHelper.java` | 날짜·요일 계산 유틸 |
| `dao/SleepStageRepository.java` | 수면 단계 조회 |
| `dao/DailyHealthSummaryRepository.java` | 수면 요약 조회 |
| `dao/AlarmRepository.java` | 알람 설정 조회/저장 |
