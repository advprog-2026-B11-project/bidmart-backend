package com.example.bidmart.user.service;

import com.example.bidmart.user.dto.AuthResponse;
import com.example.bidmart.user.dto.LoginRequest;
import com.example.bidmart.user.dto.RegisterRequest;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateNewUser(request.getUsername(), request.getEmail());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        user.setRole(Role.USER);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        return mapToAuthResponse(savedUser, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = findUserByIdentifier(request.getIdentifier());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Catatan: Gunakan Custom Exception (misal: BadCredentialsException) di tahap selanjutnya
            throw new IllegalArgumentException("Password yang Anda masukkan salah."); 
        }
        
        String dummyAccessToken = "dummy-jwt-access-token";
        String dummyRefreshToken = "dummy-jwt-refresh-token";

        return mapToAuthResponse(user, dummyAccessToken, dummyRefreshToken);
    }

    @Override
    public boolean verifyEmail(String token) {
        // TODO: Implementasi logika verifikasi email (Milestone 25%)
        // 1. Cari user berdasarkan token verifikasi
        // 2. Ubah isEmailVerified menjadi true
        // 3. Simpan perubahan
        return false;
    }

    private void validateNewUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username sudah digunakan.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email sudah terdaftar.");
        }
    }

    private User findUserByIdentifier(String identifier) {
        Optional<User> userOptional = userRepository.findByEmail(identifier);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(identifier);
        }
        
        return userOptional.orElseThrow(() -> 
            new IllegalArgumentException("Pengguna dengan identifier tersebut tidak ditemukan.")
        );
    }

    private AuthResponse mapToAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .build();
    }
}