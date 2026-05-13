package project.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 브라우저가 API 서버(예: :9000)에 직접 붙을 때 자동으로 요청하는 경로만 처리해
 * {@code NoHandlerFoundException} 및 예외 핸들러 ERROR 로그를 막는다.
 */
@RestController
public class BrowserWellKnownController {

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
