package com.example.test.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deeplink")
public class DeeplinkController {

    @GetMapping("/reset-password")
    public ResponseEntity<Void> redirectToApp(@RequestParam(required = false) String token) {
        return ResponseEntity.ok().build();
    }
}
