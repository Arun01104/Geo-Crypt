package com.Geo_crypt.Backend.Controller;

import com.Geo_crypt.Backend.Model.User;
import com.Geo_crypt.Backend.Security.JwtUtil;
import com.Geo_crypt.Backend.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "username already exists"));
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        String hashed = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());
        u.setPasswordHash(hashed);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("message", "registered"));
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var opt = userRepository.findByUsername(req.getUsername());
        if (opt.isEmpty()) return ResponseEntity.status(401).body(Map.of("message", "invalid credentials"));
        User u = opt.get();
        if (!BCrypt.checkpw(req.getPassword(), u.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "invalid credentials"));
        }
        String token = jwtUtil.generateToken(u.getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }


    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
    }


    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
