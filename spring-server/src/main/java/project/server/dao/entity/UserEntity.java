package project.server.dao.entity;

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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "`user`")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String email;
    private String password;
    private String phoneNumber;
    private String nickname;
    private String profileImage;
    private String status;

    private String fitbitUserId;
    private String fitbitAccessToken;
    private String fitbitRefreshToken;
    private LocalDateTime fitbitTokenExpiresAt;
    private LocalDateTime fitbitLastSyncedAt;

}
