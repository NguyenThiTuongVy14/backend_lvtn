package com.example.test.service;

import com.example.test.dto.StaffRegisterRequest;
import com.example.test.entity.StaffRequest;
import com.example.test.repository.StaffRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StaffRequestService {
    private final StaffRequestRepository staffRequestRepository;

    public String register(StaffRegisterRequest req, Integer authority) {
        StaffRequest entity = StaffRequest.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .personalId(req.getPersonalId())
                .dayOfBirth(req.getDayOfBirth())
                .gender(req.getGender())
                .authorityId(req.getAuthorityId())
                .avatar(req.getAvatar())
                .cccdFront(req.getCccdFront())
                .cccdBack(req.getCccdBack())
                .licenseFront(req.getLicenseFront())
                .licenseBack(req.getLicenseBack())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        staffRequestRepository.save(entity);
        return "Đăng ký thành công, chờ admin duyệt.";
    }

}
