package com.xaip.controller;

import com.xaip.dto.PredictionRequest;
import com.xaip.dto.PredictionResponse;
import com.xaip.service.PredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping(value = "/predict", consumes = { "multipart/form-data", "application/x-www-form-urlencoded" })
    public ResponseEntity<PredictionResponse> predict(
            @Valid @ModelAttribute PredictionRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                predictionService.predict(req, userDetails.getUsername()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PredictionResponse>> history(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                predictionService.getHistory(userDetails.getUsername()));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("healthy");
    }
}
