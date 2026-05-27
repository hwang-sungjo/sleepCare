package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * S3 객체로 보관되는 페르소나용 system prompt 텍스트를 TTL 인메모리 캐시로 제공한다.
 *
 * <p>
 * 같은 키에 대한 동시 호출은 {@link ConcurrentMap#compute(Object, java.util.function.BiFunction)} 으로
 * 한 번만 S3 GetObject 가 발생하도록 보장한다. TTL 이 0 이하이면 매 호출마다 S3 를 조회한다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3PromptService {

    private final S3Client s3Client;

    @Value("${app.ai.s3-bucket}")
    private String bucket;

    @Value("${app.ai.prompt-ttl-seconds}")
    private long ttlSeconds;

    private final ConcurrentMap<String, CachedPrompt> cache = new ConcurrentHashMap<>();

    /**
     * 주어진 S3 키의 텍스트(UTF-8)를 반환한다. 만료/미존재 시 1회 S3 GetObject.
     *
     * @throws IllegalStateException 버킷이 비어 있거나 S3 조회 실패 시
     */
    public String getPrompt(String s3Key) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.ai.s3-bucket is not configured");
        }
        Instant now = Instant.now();
        CachedPrompt cached = cache.compute(s3Key, (key, existing) -> {
            if (existing != null && !isExpired(existing, now)) {
                return existing;
            }
            String text = fetchFromS3(key);
            return new CachedPrompt(text, now);
        });
        return cached.text();
    }

    private boolean isExpired(CachedPrompt entry, Instant now) {
        if (ttlSeconds <= 0L) {
            return true;
        }
        return entry.loadedAt().plus(Duration.ofSeconds(ttlSeconds)).isBefore(now);
    }

    private String fetchFromS3(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
        String text = bytes.asString(StandardCharsets.UTF_8);
        log.debug("[S3PromptService] loaded prompt bucket={} key={} bytes={}", bucket, key, text.length());
        return text;
    }

    private record CachedPrompt(String text, Instant loadedAt) {
    }
}
