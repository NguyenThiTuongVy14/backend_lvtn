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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public AuthController(AuthenticationManager authenticationManager, StaffDetailsService staffDetailsService, StaffRepository staffRepository, FcmRepository fcmRepository, MailService mailService) {
        this.authenticationManager = authenticationManager;
        this.staffDetailsService = staffDetailsService;
        this.staffRepository = staffRepository;
        this.fcmRepository = fcmRepository;
        this.mailService = mailService;
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

            if (loginRequest.getFcmToken() != null && !loginRequest.getFcmToken().isEmpty()) {
                FCMToken token = new FCMToken();
                token.setStaffId(staff.getId());
                token.setToken(loginRequest.getFcmToken());
                fcmRepository.save(token);
            }
            JwtResponse response= new JwtResponse();
            response.setToken(jwt);
            response.setRole(staff.getAuthorityId());
            return ResponseEntity.ok(response);}
        else {
            return ResponseEntity.badRequest().body("{\"error\":\"Permission denied\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("{\"error\": \"Invalid username or password\"}");
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("{\"message\": \"Logout successfully\"}");
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        Staff staff = staffRepository.findByUserName(username);

        if (staff != null) {
            String email = staff.getEmail();
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body("Tài khoản không có email");
            }
            UserDetails userDetails = staffDetailsService.loadUserByUsername(username);
            String jwt = generateJwtToken(userDetails);
            mailService.sendResetPasswordLink(email, jwt);

            return ResponseEntity.ok("Đã gửi email đến: " + email);
        }
        return ResponseEntity.badRequest().body("{\"error\": \"Invalid username\"}");
    }

//    @GetMapping("/open-app/reset-password")
//    public void redirectToApp(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
//        String deepLink = "colector://reset-password?token=" + token;
//        response.sendRedirect(deepLink);
//    }

    @GetMapping("/deeplink/reset-password")
    public ResponseEntity<Void> redirectToApp(@RequestParam(required = false) String token) {
        // Trả về 200 OK (Android mới nhận)
        return ResponseEntity.ok().build();
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