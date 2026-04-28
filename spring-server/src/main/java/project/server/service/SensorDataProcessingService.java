package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.SensorDataRepository;
import project.server.dao.entity.SensorDataEntity;
import project.server.dto.sensor.PostSensorDataRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataProcessingService {

    private final SensorDataRepository sensorDataRepository;
    private final DynamicAlarmService dynamicAlarmService;

    @Transactional
    public void ingestSensorData(PostSensorDataRequest dto) {
        SensorDataEntity row = SensorDataEntity.builder()
                .userId(dto.getUserId())
                .deviceId(dto.getDeviceId())
                .recordedAt(dto.resolveRecordedAt())
                .illuminance(clampLighting(dto.getIlluminance()))
                .temperature(clampTemp(dto.getTemperature()))
                .humidity(clampHumidity(dto.getHumidity()))
                .build();
        sensorDataRepository.save(row);
        dynamicAlarmService.recalculateForUser(dto.getUserId());
        log.debug("[SensorDataProcessingService] saved sensor row for user {}", dto.getUserId());
    }

    private static Double clampLighting(Double value) {
        return value != null ? Math.max(0d, Math.min(value, 200000d)) : null;
    }

    private static Double clampTemp(Double value) {
        return value != null ? Math.max(-40d, Math.min(value, 60d)) : null;
    }

    private static Double clampHumidity(Double value) {
        return value != null ? Math.max(0d, Math.min(value, 100d)) : null;
    }
}
