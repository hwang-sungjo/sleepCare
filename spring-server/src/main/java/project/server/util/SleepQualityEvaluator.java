package project.server.util;

import project.server.entity.DailyHealthSummary;
import project.server.entity.SleepStage;

import java.util.List;
import java.util.Locale;

/**
 * 수면 품질 점수(0~100) 계산 및 탐색 창(effectiveWindow) 결정.
 *
 * <p>알고리즘 상세는 docs/ALARM_ALGORITHM.md 참조.
 */
public final class SleepQualityEvaluator {

    /** D-1 ~ D-7 순서 가중치 합계 = 1.0 */
    private static final double[] DAY_WEIGHTS = {0.30, 0.20, 0.15, 0.12, 0.10, 0.08, 0.05};

    /** effectiveWindow 최대값(분) */
    private static final int MAX_WINDOW_MINUTES = 120;

    private SleepQualityEvaluator() {}

    /**
     * daily_health_summary 1행으로 단일 야간 수면 점수(0~100)를 계산한다.
     *
     * <ul>
     *   <li>수면 효율 50%</li>
     *   <li>깊은 수면 비율 25% — minutesAsleep의 20% 이상이면 100점</li>
     *   <li>REM 비율 15%         — minutesAsleep의 20% 이상이면 100점</li>
     *   <li>각성 패널티 10%      — 각성 1분당 -2점</li>
     * </ul>
     */
    public static double computeNightScore(DailyHealthSummary dhs) {
        int minutesAsleep = dhs.getMinutesAsleep();
        if (minutesAsleep <= 0) {
            return 100.0;
        }

        double deepScore  = Math.min(dhs.getDeepMins()  / (double) minutesAsleep * 500.0, 100.0);
        double remScore   = Math.min(dhs.getRemMins()   / (double) minutesAsleep * 500.0, 100.0);
        double wakeScore  = Math.max(0.0, 100.0 - dhs.getWakeMins() * 2.0);
        double nightScore = dhs.getEfficiency() * 0.50
                          + deepScore           * 0.25
                          + remScore            * 0.15
                          + wakeScore           * 0.10;

        return Math.max(0.0, Math.min(100.0, nightScore));
    }

    /**
     * 최근 7일치 daily_health_summary 리스트(날짜 오름차순)로 가중 평균 점수를 계산한다.
     *
     * <p>리스트의 마지막 원소가 D-1(어제)에 해당한다. 데이터가 없는 날은 해당 가중치를
     * 제외하고 나머지 가중치 합으로 정규화한다.
     *
     * @param recent 날짜 오름차순 리스트 (최대 7개, D-7 ~ D-1)
     */
    public static double computeWeightedScore(List<DailyHealthSummary> recent) {
        if (recent == null || recent.isEmpty()) {
            return 100.0;
        }

        int n = recent.size();  // 최대 7
        // recent.get(n-1) = D-1, recent.get(n-2) = D-2, ...
        // DAY_WEIGHTS[0] = D-1 가중치, [1] = D-2, ...

        double weightedSum = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < n && i < DAY_WEIGHTS.length; i++) {
            // recent 리스트 오름차순 → 뒤에서부터가 최신
            DailyHealthSummary dhs = recent.get(n - 1 - i);
            double w = DAY_WEIGHTS[i];
            weightedSum += computeNightScore(dhs) * w;
            totalWeight += w;
        }

        if (totalWeight == 0.0) {
            return 100.0;
        }
        double score = weightedSum / totalWeight;
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * daily_health_summary 없을 때 sleep_stage 데이터로 간이 점수를 추정한다.
     *
     * <pre>score = 70 - deepRatio×40 + lightRatio×15 + remRatio×15</pre>
     */
    public static double computeFallbackScore(List<SleepStage> stages) {
        if (stages == null || stages.isEmpty()) {
            return 100.0;
        }

        long totalSec = 0L;
        long deepSec  = 0L;
        long lightSec = 0L;
        long remSec   = 0L;

        for (SleepStage s : stages) {
            if (s.getDurationSeconds() == null) continue;
            long dur = s.getDurationSeconds().longValue();
            totalSec += dur;

            String level = s.getStageLevel() == null
                    ? "" : s.getStageLevel().toLowerCase(Locale.ROOT);
            if (level.contains("deep"))         deepSec  += dur;
            else if (level.contains("light"))   lightSec += dur;
            else if (level.contains("rem"))     remSec   += dur;
        }

        if (totalSec <= 0L) {
            return 100.0;
        }

        double deepRatio  = deepSec  / (double) totalSec;
        double lightRatio = lightSec / (double) totalSec;
        double remRatio   = remSec   / (double) totalSec;
        double score = 70.0 - deepRatio * 40.0 + lightRatio * 15.0 + remRatio * 15.0;

        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * 수면 점수에 따라 추가 탐색 창을 결정하고 baseWindowMinutes에 더한 뒤 120분으로 캡핑한다.
     *
     * <table>
     *   <tr><th>점수</th><th>추가 분</th></tr>
     *   <tr><td>80 ~ 100</td><td>+0</td></tr>
     *   <tr><td>60 ~ 79 </td><td>+15</td></tr>
     *   <tr><td>40 ~ 59 </td><td>+30</td></tr>
     *   <tr><td>0  ~ 39 </td><td>+60</td></tr>
     * </table>
     */
    public static int effectiveWindowMinutes(int baseWindowMinutes, double score) {
        int extra;
        if (score >= 80.0)      extra = 0;
        else if (score >= 60.0) extra = 15;
        else if (score >= 40.0) extra = 30;
        else                    extra = 60;

        return Math.min(baseWindowMinutes + extra, MAX_WINDOW_MINUTES);
    }
}
