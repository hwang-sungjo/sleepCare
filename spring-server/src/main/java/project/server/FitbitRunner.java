package project.server;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import project.server.service.FitbitAuthService;
import project.server.service.FitbitSyncService;

import java.util.Scanner;

@Component
@RequiredArgsConstructor
public class FitbitRunner implements CommandLineRunner {

    private final FitbitAuthService authService;
    private final FitbitSyncService syncService;

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n==================================================");
        System.out.println("  Fitbit Cloud IoT Server - Data Pipeline");
        System.out.println("==================================================");
        System.out.print("🔑 Enter your INITIAL Refresh Token: ");
        String refreshToken = scanner.nextLine();

        System.out.println("\n🚀 Initializing Token...");
        if (authService.initializeToken(refreshToken)) {
            // 토큰 갱신 성공 시 7일치 데이터 동기화 시작
            syncService.scheduledFullSync();

            System.out.println("👉 DB 접속 주소: http://localhost:9000/h2-console");
            System.out.println("👉 JDBC URL: jdbc:h2:mem:fitbitdb (User: sa, Password: 없음)");
        } else {
            System.out.println("❌ 서버 시작 실패: Refresh Token을 다시 확인해주세요.");
        }
    }
}