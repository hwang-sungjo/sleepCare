package project.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Fitbit 수면 단계 타임라인.
 * 기상 시점에 daily 1회 일괄 적재 후 그 날에는 갱신되지 않는다.
 */
@Entity
@Table(name = "sleep_stage")
@Getter @Setter @NoArgsConstructor
public class SleepStage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDate recordDate;
    @Column(nullable = false)
    private LocalDateTime startTime;
    private Integer durationSeconds;
    private String stageLevel;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
