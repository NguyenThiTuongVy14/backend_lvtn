package com.example.test.controller;

import com.example.test.dto.JwtResponse;
import com.example.test.dto.LoginRequest;
import com.example.test.entity.FCMToken;
import com.example.test.entity.Staff;
import com.example.test.repository.FcmRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.service.MailService;
import com.example.test.service.StaffDetailsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final StaffDetailsService staffDetailsService;
    private final StaffRepository  staffRepository;
    private final FcmRepository fcmRepository;
    private final MailService mailService;
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, StaffDetailsService staffDetailsService, StaffRepository staffRepository, FcmRepository fcmRepository, MailService mailService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.staffDetailsService = staffDetailsService;
        this.staffRepository = staffRepository;
        this.fcmRepository = fcmRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            UserDetails userDetails = staffDetailsService.loadUserByUsername(loginRequest.getUsername());
            String jwt = generateJwtToken(userDetails);
            Staff staff = staffRepository.findByUserName(loginRequest.getUsername());
            if (loginRequest.getRole()==staff.getAuthorityId()){

                if (loginRequest.getFcmToken() != null && !loginRequest.getFcmToken().isEmpty() && !fcmRepository.existsByToken(loginRequest.getFcmToken())) {
                    FCMToken token = new FCMToken();
                    token.setStaffId(staff.getId());
                    token.setToken(loginRequest.getFcmToken());
                    fcmRepository.save(token);
                }
                JwtResponse response= new JwtResponse();
                response.setToken(jwt);
                response.setRole(staff.getAuthorityId());
                return ResponseEntity.ok(response);
            }
        else {
            return ResponseEntity.badRequest().body("{\"error\":\"Permission denied\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("{\"error\": \"Invalid username or password\"}");
        }
    }
    @Transactional
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String fcmToken = request.get("fcmToken");
        if (fcmToken != null && !fcmToken.isEmpty()) {
            fcmRepository.deleteByToken(fcmToken);
        }

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logout successfully"));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        Staff staff = staffRepository.findByUserName(username);

        if (staff == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tài khoản không tồn tại"));
        }

        if (staff.getEmail() == null || staff.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tài khoản không có email"));
        }

        String otp = String.valueOf((int) (Math.random() * 90000) + 10000);
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(10);

        staff.setOtp(otp);
        staff.setOtpExpiredAt(Timestamp.valueOf(expiredAt));
        staffRepository.save(staff);

        mailService.sendOtpMail(staff.getEmail(), otp);
        return ResponseEntity.ok(Map.of("message", "Đã gửi OTP đến email"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String otp = (String) body.get("otp");

        Staff staff = staffRepository.findByUserName(username);
        if (staff == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tài khoản không tồn tại"));
        }

        if (!otp.equals(staff.getOtp())) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP không đúng"));
        }

        if (staff.getOtpExpiredAt().toLocalDateTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP đã hết hạn"));
        }

        return ResponseEntity.ok(Map.of("message", "Xác thực OTP thành công"));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String otp = (String) body.get("otp");
        String newPassword = (String) body.get("newPassword");

        Staff staff = staffRepository.findByUserName(username);
        if (staff == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tài khoản không tồn tại"));
        }

        if (!otp.equals(staff.getOtp())) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP không đúng"));
        }

        if (staff.getOtpExpiredAt().toLocalDateTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP đã hết hạn"));
        }

        staff.setPassword(passwordEncoder.encode(newPassword));
        staff.setOtp(null);
        staff.setOtpExpiredAt(null);
        staffRepository.save(staff);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }






    private String generateJwtToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}