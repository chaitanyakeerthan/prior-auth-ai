package com.xaip.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PredictionRequest {

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 150, message = "Age must be at most 150")
    private Integer age;

    @NotBlank(message = "Diagnosis is required")
    @Size(max = 500, message = "Diagnosis must be at most 500 characters")
    private String diagnosis;

    @NotBlank(message = "Treatment is required")
    @Size(max = 500, message = "Treatment must be at most 500 characters")
    private String treatment;

    private String provider;
    private String priority;
    private String clinicalNotes;
    private org.springframework.web.multipart.MultipartFile document;
}
