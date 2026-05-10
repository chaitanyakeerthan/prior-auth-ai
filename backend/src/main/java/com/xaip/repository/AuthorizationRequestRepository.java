package com.xaip.repository;

import com.xaip.entity.AuthorizationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuthorizationRequestRepository extends JpaRepository<AuthorizationRequest, Long> {
    List<AuthorizationRequest> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}
