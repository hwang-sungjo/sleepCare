package project.server.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;

/** 사용자별·요일별 1건씩 존재하는 적응형 알람 설정. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "alarm")
public class AlarmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmId;

    private Long userId;

    /** ISO 요일 (1=월 … 7=일). 사용자당 동일 번호 중복 불가. */
    private Integer dayOfWeek;

    /**
     * 매주 해당 요일에 적용되는 목표 벽시계 시각(DB TIME; 애플리케이션에서는 Asia/Seoul 로 해석).
     */
    private LocalTime baseWakeTime;

    /**
     * 그 요일 행 기준 현재 채택된 실제 알람 발생 후보 시각(UTC 기준 {@link Instant}).
     */
    @Column(nullable = false)
    private Instant dynamicWakeAt;

    private Boolean adaptiveEnabled;
    private Integer windowMinutesBefore;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

}
