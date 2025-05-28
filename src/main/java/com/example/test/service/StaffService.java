package com.example.test.service;

import com.example.test.entity.Staff;
import com.example.test.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    @Autowired
    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    // Tạo nhân viên mới
    @Transactional
    public Staff createStaff(Staff staff) {
        // Kiểm tra dữ liệu đầu vào
        if (staff.getUserName() == null || staff.getUserName().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (staff.getAuthorityId() == null) {
            throw new IllegalArgumentException("Authority ID cannot be empty");
        }
        if (staffRepository.findByUserName(staff.getUserName()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (staff.getStaffCode() == null || staff.getStaffCode().isEmpty()) {
            throw new IllegalArgumentException("Staff code cannot be empty");
        }

        // Đặt trạng thái mặc định nếu chưa có
        if (staff.getStatus() == null) {
            staff.setStatus("ACTIVE"); // Giả định trạng thái mặc định
        }

        return staffRepository.save(staff);
    }

    // Cập nhật thông tin nhân viên
    @Transactional
    public Staff updateStaff(Integer id, Staff staff) {
        Optional<Staff> existingStaff = staffRepository.findById(id);
        if (existingStaff.isEmpty()) {
            throw new IllegalArgumentException("Staff with ID " + id + " not found");
        }

        Staff staffToUpdate = existingStaff.get();

        // Cập nhật các trường được phép
        if (staff.getStaffCode() != null) {
            staffToUpdate.setStaffCode(staff.getStaffCode());
        }
        if (staff.getFullName() != null) {
            staffToUpdate.setFullName(staff.getFullName());
        }
        if (staff.getDayOfBirth() != null) {
            staffToUpdate.setDayOfBirth(staff.getDayOfBirth());
        }
        if (staff.getPhone() != null) {
            staffToUpdate.setPhone(staff.getPhone());
        }
        if (staff.getAddress() != null) {
            staffToUpdate.setAddress(staff.getAddress());
        }
        if (staff.getPersonalId() != null) {
            staffToUpdate.setPersonalId(staff.getPersonalId());
        }
        if (staff.getGender() != null) {
            staffToUpdate.setGender(staff.getGender());
        }
        if (staff.getStartDay() != null) {
            staffToUpdate.setStartDay(staff.getStartDay());
        }
        if (staff.getEndDay() != null) {
            staffToUpdate.setEndDay(staff.getEndDay());
        }
        if (staff.getEmail() != null) {
            staffToUpdate.setEmail(staff.getEmail());
        }
        if (staff.getPassword() != null) {
            staffToUpdate.setPassword(staff.getPassword()); // Lưu ý: Nên mã hóa mật khẩu
        }
        if (staff.getStatus() != null) {
            staffToUpdate.setStatus(staff.getStatus());
        }
        if (staff.getAuthorityId() != null) {
            staffToUpdate.setAuthorityId(staff.getAuthorityId());
        }

        return staffRepository.save(staffToUpdate);
    }

    // Cập nhật profile của nhân viên hiện tại
    @Transactional
    public Staff updateStaffProfile(Integer id, Staff staffUpdate) {
        Optional<Staff> existingStaff = staffRepository.findById(id);
        if (existingStaff.isEmpty()) {
            throw new IllegalArgumentException("Staff with ID " + id + " not found");
        }

        Staff staffToUpdate = existingStaff.get();

        // Chỉ cho phép cập nhật các trường không nhạy cảm
        if (staffUpdate.getFullName() != null) {
            staffToUpdate.setFullName(staffUpdate.getFullName());
        }
        if (staffUpdate.getPhone() != null) {
            staffToUpdate.setPhone(staffUpdate.getPhone());
        }
        if (staffUpdate.getAddress() != null) {
            staffToUpdate.setAddress(staffUpdate.getAddress());
        }
        if (staffUpdate.getEmail() != null) {
            staffToUpdate.setEmail(staffUpdate.getEmail());
        }
        if (staffUpdate.getDayOfBirth() != null) {
            staffToUpdate.setDayOfBirth(staffUpdate.getDayOfBirth());
        }
        if (staffUpdate.getGender() != null) {
            staffToUpdate.setGender(staffUpdate.getGender());
        }

        return staffRepository.save(staffToUpdate);
    }

    // Xóa nhân viên
    @Transactional
    public void deleteStaff(Integer id) {
        Optional<Staff> existingStaff = staffRepository.findById(id);
        if (existingStaff.isEmpty()) {
            throw new IllegalArgumentException("Staff with ID " + id + " not found");
        }
        Staff staffToUpdate = existingStaff.get();
        staffToUpdate.setStatus("DELETED");
        staffRepository.save(staffToUpdate);
    }
}