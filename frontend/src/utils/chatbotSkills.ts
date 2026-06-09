const SKILL_LABELS: Record<string, string> = {
    get_daily_sleep_summary: '일일 수면 요약 조회',
    get_sleep_trend_analysis: '주간/월간 수면 추세',
    get_sleep_efficiency_ranking: '수면 효율 비교',
    match_environment_with_sleep_stages: '환경-수면 단계 매칭',
    analyze_light_sensitivity: '조도-기상 민감도',
    get_cardiovascular_metrics: '심혈관 지표',
    check_respiratory_health: '호흡/SpO2',
    track_skin_temperature: '피부 온도 추이',
    evaluate_adaptive_alarm_performance: '적응형 알람 점검',
    assess_sleep_regularity: '수면 규칙성',
};

export function formatToolCallLabel(skillId: string, status: string): string {
    const label = SKILL_LABELS[skillId] ?? skillId;
    const statusLabel = status === 'ok' ? '성공' : '실패';
    return `${label} ${statusLabel}`;
}
