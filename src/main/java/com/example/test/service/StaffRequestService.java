package com.example.test.service;

import com.example.test.dto.StaffRegisterRequest;
import com.example.test.entity.StaffRequest;
import com.example.test.repository.StaffRepository;
import com.example.test.repository.StaffRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffRequestService {
    private final StaffRequestRepository staffRequestRepository;

    public boolean register(
            StaffRegisterRequest req,
            Integer authority,
            MultipartFile avatar,
            MultipartFile cccdFront,
            MultipartFile cccdBack,
            MultipartFile licenseFront,
            MultipartFile licenseBack) {
            if(staffRequestRepository.existsByEmail(req.getEmail())){
                return false;
            }

        String avatarPath = saveFile(avatar, "avatar");
        String cccdFrontPath = saveFile(cccdFront, "cccdFront");
        String cccdBackPath = saveFile(cccdBack, "cccdBack");
        String licenseFrontPath = saveFile(licenseFront, "licenseFront");
        String licenseBackPath = saveFile(licenseBack, "licenseBack");

        StaffRequest entity = StaffRequest.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .personalId(req.getPersonalId())
                .dayOfBirth(LocalDate.parse(req.getDayOfBirth()))
                .gender(req.getGender())
                .authorityId(authority)
                .avatar(avatarPath)
                .cccdFront(cccdFrontPath)
                .cccdBack(cccdBackPath)
                .licenseFront(licenseFrontPath)
                .licenseBack(licenseBackPath)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        staffRequestRepository.save(entity);
        return true;
    }

    private String saveFile(MultipartFile file, String folderName) {
        if (file != null && !file.isEmpty()) {
            try {
                // Lấy đường dẫn tuyệt đối đến thư mục gốc dự án (hoặc có thể đặt nơi khác nếu cần)
                String baseDir = new File("uploads").getAbsolutePath();
                String uploadsDir = baseDir + File.separator + folderName;

                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();

                String originalName = file.getOriginalFilename();
                String extension = originalName != null ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String fileName = UUID.randomUUID() + extension;
                File dest = new File(dir, fileName);
                file.transferTo(dest);

                // Trả về path tương đối nếu cần dùng sau này (ví dụ frontend hiển thị)
                return "uploads/" + folderName + "/" + fileName;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
