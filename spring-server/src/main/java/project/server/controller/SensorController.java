package project.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.common.exception.UserException;
import project.server.common.response.BaseResponse;
import project.server.dto.sensor.PostSensorDataRequest;
import project.server.service.SensorDataProcessingService;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static project.server.common.response.status.BaseExceptionResponseStatus.INVALID_USER_VALUE;
import static project.server.util.BindingResultUtils.getErrorMessages;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sensors")
public class SensorController {

    private final SensorDataProcessingService sensorDataProcessingService;

    @PostMapping("/data")
    public BaseResponse<String> ingest(@Validated @RequestBody PostSensorDataRequest request,
            BindingResult bindingResult) {
        log.info("[SensorController.ingest] userId={}", request.getUserId());
        if (bindingResult.hasErrors()) {
            throw new UserException(INVALID_USER_VALUE, getErrorMessages(bindingResult));
        }
        sensorDataProcessingService.ingestSensorData(request);
        return new BaseResponse<>("accepted");
    }
}
