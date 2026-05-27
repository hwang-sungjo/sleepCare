package project.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS SDK v2 클라이언트 빈 설정.
 *
 * <p>
 * 두 클라이언트 모두 {@code app.ai.region} 리전 위에서 기본 자격증명 제공자 체인
 * (환경변수, 컨테이너/EC2 인스턴스 프로파일, IAM 역할 등)을 사용한다.
 * 자격증명이 없거나 잘못된 경우라도 빈 생성 자체는 성공하며,
 * 실제 호출 시점에 SDK가 예외를 던진다 — AI 기능 외 API는 영향을 받지 않는다.
 * </p>
 */
@Configuration
public class AwsClientConfig {

    @Bean
    public S3Client s3Client(@Value("${app.ai.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public BedrockAgentRuntimeClient bedrockAgentRuntimeClient(@Value("${app.ai.region}") String region) {
        return BedrockAgentRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(@Value("${app.ai.region}") String region) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
