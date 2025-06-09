package com.example.test.dto;

import com.example.test.entity.Staff;
import lombok.Data;
import java.time.LocalDate;

@Data
public class StaffDTO {
    private Integer id;
    private String staffCode;
    private String fullName;
    private LocalDate dayOfBirth;
    private String phone;
    private String address;
    private String personalId;
    private Integer gender;
    private LocalDate startDay;
    private LocalDate endDay;
    private String userName;
    private String email;
    private String status;
    private Integer authorityId;
    private String authorityName;

    // Constructor từ Staff entity
    public StaffDTO(Staff staff) {
        this.id = staff.getId();
        this.staffCode = staff.getStaffCode();
        this.fullName = staff.getFullName();
        this.dayOfBirth = staff.getDayOfBirth();
        this.phone = staff.getPhone();
        this.address = staff.getAddress();
        this.personalId = staff.getPersonalId();
        this.gender = staff.getGender();
        this.startDay = staff.getStartDay();
        this.endDay = staff.getEndDay();
        this.userName = staff.getUserName();
        this.email = staff.getEmail();
        this.status = staff.getStatus();
        this.authorityId = staff.getAuthorityId();
    }

    public StaffDTO(Integer staffId, String staffName, String phone, String email) {
        this.id = staffId;
        this.fullName = staffName;
        this.phone = phone;
        this.email = email;
    }
}