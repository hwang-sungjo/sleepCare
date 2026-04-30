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

import java.time.Instant;
import java.time.LocalDate;

/**
 * Fitbit 1분 단위 심박수 기록.
 * 기상 시점에 daily 1회 일괄 적재 후 그 날에는 갱신되지 않는다.
 */
@Entity
@Table(name = "heart_rate")
@Getter @Setter @NoArgsConstructor
public class HeartRate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDate recordDate;
    @Column(nullable = false)
    private Instant recordTime;
    private Integer bpm;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
