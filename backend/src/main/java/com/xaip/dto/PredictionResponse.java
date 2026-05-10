package com.xaip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @Builder
public class PredictionResponse {
    private Long id;
    private Integer age;
    private String diagnosis;
    private String treatment;
    private Double approvalPercentage;
    private String status;
    private String confidenceLevel;
    private List<String> reasons;
    private List<String> suggestions;
    private LocalDateTime createdAt;
}
