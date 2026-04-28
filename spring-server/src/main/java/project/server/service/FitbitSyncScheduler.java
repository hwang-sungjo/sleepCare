package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.dao.entity.UserEntity;
import project.server.dao.UserRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FitbitSyncScheduler {

    private final UserRepository userRepository;
    private final FitbitIntegrationService fitbitIntegrationService;

    /** Nightly aggregated sync for OAuth-linked Fitbit accounts (Seoul cron window). */
    @Scheduled(cron = "${app.fitbit-sync-cron:0 0 6 * * ?}")
    public void syncLinkedUsers() {
        List<UserEntity> linked = userRepository.findUsersWithStoredFitbitCredential();
        for (UserEntity user : linked) {
            try {
                fitbitIntegrationService.syncYesterdayForUser(user.getUserId());
            } catch (Exception ex) {
                log.warn(
                        "[FitbitSyncScheduler] sync skipped user={} reason={}",
                        user.getUserId(),
                        ex.getMessage(),
                        ex);
            }
        }
    }
}
