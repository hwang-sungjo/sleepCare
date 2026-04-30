package project.server.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fitbit")
public class FitbitEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "fitbit_access_token", columnDefinition = "TEXT")
    private String fitbitAccessToken;

    @Column(name = "fitbit_refresh_token", columnDefinition = "TEXT")
    private String fitbitRefreshToken;

    private Instant fitbitTokenExpiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
