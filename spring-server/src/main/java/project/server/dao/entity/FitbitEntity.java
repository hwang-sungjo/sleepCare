package project.server.dao.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fitbit")
public class FitbitEntity {

    @Id
    private String fitbitUserId;

    private Long userId;
    private String fitbitUserPassword;
    private String fitbitAccessToken;
    private String fitbitRefreshToken;
    private LocalDateTime fitbitTokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
