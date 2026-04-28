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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fitbit_data")
public class FitbitDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fitbitDataId;

    private Long userId;
    private Instant segmentStart;
    private Instant segmentEnd;
    private String sleepStage;

    /** Heart rate variability in milliseconds where available */
    private BigDecimal hrvMs;

    private Integer restingHrBpm;
    private String payloadJson;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

}
