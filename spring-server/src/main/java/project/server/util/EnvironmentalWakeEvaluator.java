package project.server.util;

import project.server.dao.entity.RealtimeMetricEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기상 직전 1시간의 심박수·조도·온도·습도 4요인으로 기상 쾌적도 점수(0~100)를 계산한다.
 *
 * <p>알고리즘 상세는 docs/ALARM_ALGORITHM.md 참조.
 *
 * <h3>가중치</h3>
 * <ul>
 *   <li>심박수 40% — 수면 깊이를 가장 직접적으로 반영</li>
 *   <li>조도   25% — 코르티솔·멜라토닌을 통한 강한 생체시계 신호</li>
 *   <li>온도   20% — 자연 기상 쾌적 범위(18~21°C)</li>
 *   <li>습도   15% — 수면·기상 쾌적도 보조 지표(40~60%)</li>
 * </ul>
 */
public final class EnvironmentalWakeEvaluator {

    static final double W_HR       = 0.40;
    static final double W_LUX      = 0.25;
    static final double W_TEMP     = 0.20;
    static final double W_HUMIDITY = 0.15;

    /** IoT–HR 타임스탬프 매칭 허용 오차 (분). */
    static final long IOT_JOIN_TOLERANCE_MINUTES = 5L;

    /** 해당 요인 데이터 없을 때 사용하는 중립 점수. */
    public static final double NEUTRAL_SCORE = 50.0;

    private EnvironmentalWakeEvaluator() {}

    /**
     * 심박수 점수.
     *
     * <p>수면 최저 심박(sleepMinBpm) 대비 20 bpm 상승 시 100점.
     * 기상 직전 심박 상승은 얕은 수면·자연 기상 신호를 의미한다.
     */
    public static double scoreHr(double bpm, double sleepMinBpm) {
        return Math.max(0.0, Math.min(100.0, (bpm - sleepMinBpm) / 20.0 * 100.0));
    }

    /**
     * 조도 점수 (로그 스케일).
     *
     * <p>인간의 빛 인식이 로그적이므로 log₁₀(lux+1)/log₁₀(501)×100으로 환산.
     * 0 lux=0점, 10 lux≈21점, 100 lux≈74점, 500+ lux=100점.
     */
    public static double scoreLux(double lux) {
        if (lux <= 0.0) return 0.0;
        return Math.min(100.0, Math.log10(lux + 1.0) / Math.log10(501.0) * 100.0);
    }

    /**
     * 온도 점수.
     *
     * <p>최적 기상 온도 18~21°C (중심 19.5°C), 1°C 벗어날 때마다 10점 감점.
     */
    public static double scoreTemp(double temp) {
        return Math.max(0.0, Math.min(100.0, 100.0 - Math.abs(temp - 19.5) * 10.0));
    }

    /**
     * 습도 점수.
     *
     * <p>최적 범위 40~60% (중심 50%), 1% 벗어날 때마다 2점 감점.
     */
    public static double scoreHumidity(double humidity) {
        return Math.max(0.0, Math.min(100.0, 100.0 - Math.abs(humidity - 50.0) * 2.0));
    }

    /**
     * 4요인 가중 종합 점수.
     * HR 40% + 조도 25% + 온도 20% + 습도 15%.
     */
    public static double combinedScore(double hrScore, double luxScore, double tempScore, double humScore) {
        return hrScore * W_HR + luxScore * W_LUX + tempScore * W_TEMP + humScore * W_HUMIDITY;
    }

    /**
     * HR 데이터 없을 때 나머지 3요인(조도·온도·습도)만으로 종합 점수를 계산한다.
     * 각 가중치를 합(0.60)으로 나눠 정규화한다.
     */
    public static double combinedScoreWithoutHr(double luxScore, double tempScore, double humScore) {
        double totalW = W_LUX + W_TEMP + W_HUMIDITY;
        return (luxScore * W_LUX + tempScore * W_TEMP + humScore * W_HUMIDITY) / totalW;
    }

    /**
     * {@code target} 시각과 가장 가까운 IoT 행을 반환한다.
     * {@value IOT_JOIN_TOLERANCE_MINUTES}분 이내에 없으면 null.
     */
    public static RealtimeMetricEntity findNearestIot(List<RealtimeMetricEntity> iotData, LocalDateTime target) {
        return iotData.stream()
                .filter(r -> Duration.between(r.getCreatedAt(), target).abs().toMinutes()
                        <= IOT_JOIN_TOLERANCE_MINUTES)
                .min((a, b) -> Long.compare(
                        Duration.between(a.getCreatedAt(), target).abs().toMinutes(),
                        Duration.between(b.getCreatedAt(), target).abs().toMinutes()))
                .orElse(null);
    }
}
