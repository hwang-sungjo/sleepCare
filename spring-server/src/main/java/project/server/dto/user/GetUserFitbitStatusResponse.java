package project.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetUserFitbitStatusResponse {

    private boolean linked;
    private String fitbitUserId;
    private LocalDateTime lastSyncedAt;
}
