package com.example.test.controller;

import com.example.test.dto.ResponseMessage;
import com.example.test.dto.StaffRegisterRequest;
import com.example.test.service.StaffRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class StaffRequestController {

    private final StaffRequestService staffRequestService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/driver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerDriver(
            @RequestPart("data") String requestJson,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestPart(value = "cccdBack", required = false) MultipartFile cccdBack,
            @RequestPart(value = "licenseFront", required = false) MultipartFile licenseFront,
            @RequestPart(value = "licenseBack", required = false) MultipartFile licenseBack
    ) {
        try {
            StaffRegisterRequest request = objectMapper.readValue(requestJson, StaffRegisterRequest.class);
            if(staffRequestService.register(request, 2, avatar, cccdFront, cccdBack, licenseFront, licenseBack))
                return ResponseEntity.ok(new ResponseMessage("Đã đăng ký thành công",null));
            return ResponseEntity.badRequest().body("Đăng ký thất bại");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi dữ liệu: " + e.getMessage());
        }
    }

    @PostMapping(value = "/collector", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerCollector(
            @RequestPart("data") String requestJson,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestPart(value = "cccdBack", required = false) MultipartFile cccdBack,
            @RequestPart(value = "licenseFront", required = false) MultipartFile licenseFront,
            @RequestPart(value = "licenseBack", required = false) MultipartFile licenseBack
    ) {
        try {
            StaffRegisterRequest request = objectMapper.readValue(requestJson, StaffRegisterRequest.class);
            if(staffRequestService.register(request, 1, avatar, cccdFront, cccdBack, licenseFront, licenseBack))
                return ResponseEntity.ok(new ResponseMessage("Đã đăng ký thành công",null));
            return ResponseEntity.badRequest().body("Đăng ký thất bại");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi dữ liệu: " + e.getMessage());
        }
    }
}
