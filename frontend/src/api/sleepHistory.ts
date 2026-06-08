import { DailySleepRecord } from '../types';

const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'];

/** Mock: 최근 7일치 수면 데이터를 반환한다. 실제 API 준비 시 이 함수만 교체하면 된다. */
export function getWeeklySleepHistory(): Promise<DailySleepRecord[]> {
    return new Promise((resolve) => {
        setTimeout(() => {
            const records: DailySleepRecord[] = [];
            const today = new Date();

            // 6일 전 ~ 오늘 순서로 생성
            for (let i = 6; i >= 0; i--) {
                const date = new Date(today);
                date.setDate(today.getDate() - i);
                const dateStr = date.toISOString().slice(0, 10);
                const dayLabel = DAY_LABELS[date.getDay()];

                // 현실적인 랜덤 수면 데이터 생성
                const efficiency = Math.floor(72 + Math.random() * 24); // 72~95%
                const deepMins = Math.floor(60 + Math.random() * 40);
                const remMins = Math.floor(70 + Math.random() * 40);
                const lightMins = Math.floor(120 + Math.random() * 60);
                const wakeMins = Math.floor(10 + Math.random() * 25);
                const sleepDurationMinutes = deepMins + remMins + lightMins;

                // 취침 시간: 22시~01시 사이
                const sleepHour = Math.random() > 0.5 ? 22 + Math.floor(Math.random() * 2) : Math.floor(Math.random() * 2);
                const sleepMin = Math.floor(Math.random() * 60);
                const sleepStartTime = `${String(sleepHour).padStart(2, '0')}:${String(sleepMin).padStart(2, '0')}`;

                // 기상 시간: 취침 + 수면 시간
                const wakeMinutes = sleepHour * 60 + sleepMin + sleepDurationMinutes + wakeMins;
                const wakeHour = Math.floor(wakeMinutes / 60) % 24;
                const wakeMin = wakeMinutes % 60;
                const sleepEndTime = `${String(wakeHour).padStart(2, '0')}:${String(wakeMin).padStart(2, '0')}`;

                records.push({
                    date: dateStr,
                    dayLabel,
                    sleepEfficiency: efficiency,
                    sleepDurationMinutes,
                    sleepStartTime,
                    sleepEndTime,
                    deepMins,
                    remMins,
                    lightMins,
                    wakeMins,
                });
            }
            resolve(records);
        }, 500);
    });
}
