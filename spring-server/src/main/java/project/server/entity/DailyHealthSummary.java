package project.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 사용자별/날짜별 1행으로 유지되는 일일 수면/호흡/체온 요약.
 * 기상 시점에 한 번 적재된 뒤 그 날에는 더 이상 갱신되지 않는다.
 * Fitbit 동기화 시 본 수면(main sleep) 데이터가 충분할 때만 적재한다.
 */
@Entity
@Table(name = "daily_health_summary")
@IdClass(DailyHealthSummary.PK.class)
@Getter
@Setter
@NoArgsConstructor
public class DailyHealthSummary {

    @Id
    private Long userId;

    @Id
    private LocalDate recordDate;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Column(nullable = false)
    private int timeInBed;

    @Column(nullable = false)
    private int minutesAsleep;

    @Column(nullable = false)
    private int minutesAwake;

    @Column(nullable = false)
    private int efficiency;

    @Column(nullable = false)
    private int deepMins;

    @Column(nullable = false)
    private int lightMins;

    @Column(nullable = false)
    private int remMins;

    @Column(nullable = false)
    private int wakeMins;

    @Column(nullable = false)
    private double breathingRate;

    @Column(nullable = false)
    private double skinTempRelative;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Composite primary key for {@link DailyHealthSummary}. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PK implements Serializable {
        private Long userId;
        private LocalDate recordDate;

        public PK(Long userId, LocalDate recordDate) {
            this.userId = userId;
            this.recordDate = recordDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof PK pk))
                return false;
            return Objects.equals(userId, pk.userId) && Objects.equals(recordDate, pk.recordDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, recordDate);
        }
    }
}
