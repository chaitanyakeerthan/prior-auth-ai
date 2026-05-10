package com.xaip.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.xaip.dto.AuthResponse;
import com.xaip.dto.GoogleAuthRequest;
import com.xaip.dto.LoginRequest;
import com.xaip.dto.RegisterRequest;
import com.xaip.entity.User;
import com.xaip.repository.UserRepository;
import com.xaip.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${google.client.id}")
    private String googleClientId;

    /**
     * Register a new user, hash the password, and return a JWT.
     */
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    /**
     * Authenticate credentials and return a JWT.
     */
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    /**
     * Authenticate via Google ID Token.
     */
    public AuthResponse googleLogin(GoogleAuthRequest req) {
        try {
            // Mock behavior if client ID is not configured
            if ("YOUR_GOOGLE_CLIENT_ID".equals(googleClientId)) {
                System.out.println("Mocking Google Auth since Client ID is not configured.");
                return processGoogleUser("mock@google.com", "Mock Google User");
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(req.getToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                
                return processGoogleUser(email, name);
            } else {
                throw new IllegalArgumentException("Invalid Google ID token.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Google authentication failed: " + e.getMessage());
        }
    }

    private AuthResponse processGoogleUser(String email, String name) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;
        
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            // Register new user
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .password(passwordEncoder.encode("GOOGLE_LOGIN_NO_PASSWORD_" + System.currentTimeMillis()))
                    .build();
            userRepository.save(user);
        }

        String jwt = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(jwt)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
