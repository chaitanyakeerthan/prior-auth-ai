package com.xaip.service;

import com.xaip.dto.PredictionRequest;
import com.xaip.dto.PredictionResponse;
import com.xaip.entity.AuthorizationRequest;
import com.xaip.entity.Suggestion;
import com.xaip.entity.User;
import com.xaip.repository.AuthorizationRequestRepository;
import com.xaip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final AuthorizationRequestRepository requestRepository;
    private final UserRepository userRepository;

    // ── Knowledge base ──────────────────────────────────────────

    private static final Set<String> HIGH_APPROVAL = Set.of(
            "insulin therapy", "physical therapy", "blood test", "x-ray",
            "vaccination", "antibiotics", "routine checkup",
            "cholesterol medication", "blood pressure medication",
            "inhaler", "allergy medication"
    );

    private static final Set<String> MODERATE_TREATMENTS = Set.of(
            "mri scan", "ct scan", "ultrasound", "echocardiogram",
            "colonoscopy", "endoscopy", "dermatology consultation",
            "psychiatric evaluation", "sleep study", "cardiac stress test",
            "hormone therapy", "dialysis"
    );

    private static final Set<String> LOW_APPROVAL = Set.of(
            "cosmetic surgery", "experimental drug trial", "elective surgery",
            "weight loss surgery", "hair transplant", "laser eye surgery", "rhinoplasty"
    );

    private static final Map<String, Set<String>> DIAGNOSIS_TREATMENT_MAP = Map.ofEntries(
            Map.entry("type 2 diabetes", Set.of("insulin therapy", "blood test", "routine checkup")),
            Map.entry("type 1 diabetes", Set.of("insulin therapy", "blood test", "routine checkup")),
            Map.entry("hypertension", Set.of("blood pressure medication", "blood test", "echocardiogram")),
            Map.entry("asthma", Set.of("inhaler", "blood test", "x-ray", "allergy medication")),
            Map.entry("fracture", Set.of("x-ray", "physical therapy")),
            Map.entry("high cholesterol", Set.of("cholesterol medication", "blood test")),
            Map.entry("obesity", Set.of("routine checkup", "blood test", "physical therapy")),
            Map.entry("depression", Set.of("psychiatric evaluation", "routine checkup")),
            Map.entry("anxiety", Set.of("psychiatric evaluation", "routine checkup")),
            Map.entry("back pain", Set.of("physical therapy", "mri scan", "x-ray")),
            Map.entry("knee injury", Set.of("physical therapy", "mri scan", "x-ray")),
            Map.entry("cancer", Set.of("ct scan", "mri scan", "blood test")),
            Map.entry("heart disease", Set.of("echocardiogram", "cardiac stress test", "blood test")),
            Map.entry("kidney disease", Set.of("dialysis", "blood test", "ultrasound")),
            Map.entry("allergies", Set.of("allergy medication", "blood test"))
    );

    // ── Predict ─────────────────────────────────────────────────

    @Transactional
    public PredictionResponse predict(PredictionRequest req, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String diag = req.getDiagnosis().trim().toLowerCase();
        String treat = req.getTreatment().trim().toLowerCase();
        int age = req.getAge();

        double probability = 50.0;
        List<String> reasons = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // ── 1. Treatment category ───────────────────────────────
        if (HIGH_APPROVAL.contains(treat)) {
            probability += 35;
            reasons.add("\"" + req.getTreatment() + "\" is a commonly approved standard treatment.");
        } else if (MODERATE_TREATMENTS.contains(treat)) {
            probability += 10;
            reasons.add("\"" + req.getTreatment() + "\" typically requires supporting documentation for approval.");
            suggestions.add("Attach recent lab results or imaging reports to strengthen the request.");
        } else if (LOW_APPROVAL.contains(treat)) {
            probability -= 25;
            reasons.add("\"" + req.getTreatment() + "\" is frequently classified as elective/cosmetic and often denied.");
            suggestions.add("Provide a letter of medical necessity from the treating physician.");
        } else {
            reasons.add("\"" + req.getTreatment() + "\" is not in our common treatment database — review may take longer.");
            suggestions.add("Include detailed clinical notes explaining why this treatment is necessary.");
        }

        // ── 2. Surgery keyword check ────────────────────────────
        if (treat.contains("surgery")) {
            probability -= 10;
            reasons.add("Surgical procedures require additional pre-authorization review and documentation.");
            suggestions.add("Submit pre-operative assessment and surgeon's recommendation letter.");
        }

        // ── 3. Diagnosis ↔ treatment alignment ─────────────────
        Set<String> approvedForDiag = DIAGNOSIS_TREATMENT_MAP.get(diag);
        if (approvedForDiag != null) {
            if (approvedForDiag.contains(treat)) {
                probability += 15;
                reasons.add("\"" + req.getTreatment() + "\" is a recognized treatment for \"" + req.getDiagnosis() + "\".");
            } else {
                probability -= 10;
                reasons.add("\"" + req.getTreatment() + "\" is not a standard treatment for \"" + req.getDiagnosis() + "\".");
                String better = approvedForDiag.stream()
                        .limit(3)
                        .map(this::capitalize)
                        .collect(Collectors.joining(", "));
                suggestions.add("Consider standard treatments for this diagnosis: " + better + ".");
            }
        } else {
            suggestions.add("Provide thorough clinical documentation as this diagnosis needs manual review.");
        }

        // ── 4. Age factor ───────────────────────────────────────
        if (age > 60) {
            probability -= 8;
            reasons.add("Patients over 60 may face stricter pre-authorization criteria for certain treatments.");
            suggestions.add("Include comprehensive medical history and comorbidity documentation.");
        } else if (age < 18) {
            probability += 5;
            reasons.add("Pediatric patients often receive priority approval.");
        }

        // ── 5. Missing/vague diagnosis ──────────────────────────
        if (diag.length() < 3) {
            probability -= 20;
            reasons.add("Diagnosis appears incomplete or vague — likely to be rejected.");
            suggestions.add("Provide a specific ICD-10 coded diagnosis for faster processing.");
        }

        // ── Clamp & classify ────────────────────────────────────
        probability = Math.max(5, Math.min(98, probability));

        String status;
        String confidenceLevel;

        if (probability >= 75) {
            status = "Likely Approved";
            confidenceLevel = "High";
        } else if (probability >= 45) {
            status = "Needs Review";
            confidenceLevel = "Medium";
        } else {
            status = "Likely Denied";
            confidenceLevel = "Low";
        }

        if (suggestions.isEmpty()) {
            suggestions.add("No additional actions needed — request looks strong.");
        }

        // ── Persist ─────────────────────────────────────────────
        AuthorizationRequest record = AuthorizationRequest.builder()
                .age(age)
                .diagnosis(req.getDiagnosis())
                .treatment(req.getTreatment())
                .approvalPercentage(probability)
                .status(status)
                .confidenceLevel(confidenceLevel)
                .reasons(String.join(" | ", reasons))
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        // Add suggestions as child entities
        for (String s : suggestions) {
            Suggestion sug = Suggestion.builder()
                    .text(s)
                    .request(record)
                    .build();
            record.getSuggestions().add(sug);
        }

        requestRepository.save(record);

        return PredictionResponse.builder()
                .id(record.getId())
                .age(age)
                .diagnosis(req.getDiagnosis())
                .treatment(req.getTreatment())
                .approvalPercentage(probability)
                .status(status)
                .confidenceLevel(confidenceLevel)
                .reasons(reasons)
                .suggestions(suggestions)
                .createdAt(record.getCreatedAt())
                .build();
    }

    // ── History ─────────────────────────────────────────────────

    public List<PredictionResponse> getHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return requestRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(r -> PredictionResponse.builder()
                        .id(r.getId())
                        .age(r.getAge())
                        .diagnosis(r.getDiagnosis())
                        .treatment(r.getTreatment())
                        .approvalPercentage(r.getApprovalPercentage())
                        .status(r.getStatus())
                        .confidenceLevel(r.getConfidenceLevel())
                        .reasons(Arrays.asList(r.getReasons().split(" \\| ")))
                        .suggestions(r.getSuggestions().stream()
                                .map(Suggestion::getText)
                                .collect(Collectors.toList()))
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
