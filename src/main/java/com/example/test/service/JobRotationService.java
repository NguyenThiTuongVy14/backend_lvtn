package com.example.test.service;

import com.example.test.entity.JobPosition;
import com.example.test.entity.JobRotation;
import com.example.test.entity.Staff;
import com.example.test.entity.Vehicle;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.JobRotationRepository;
import com.example.test.repository.StaffRepository;
import com.example.test.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class JobRotationService {

    private final JobRotationRepository jobRotationRepository;
    private final StaffRepository staffRepository;
    private final JobPositionRepository jobPositionRepository;
    private final VehicleRepository vehicleRepository;

    @Autowired
    public JobRotationService(JobRotationRepository jobRotationRepository,
                              StaffRepository staffRepository,
                              JobPositionRepository jobPositionRepository,
                              VehicleRepository vehicleRepository) {
        this.jobRotationRepository = jobRotationRepository;
        this.staffRepository = staffRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // Tạo lịch phân công mới
    @Transactional
    public JobRotation createJobRotation(JobRotation jobRotation) {
        validateJobRotation(jobRotation);

        // Lấy ID nhân viên hiện tại từ SecurityContext
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Staff> currentUser = Optional.ofNullable(staffRepository.findByUserName(username));
        if (currentUser.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin người dùng hiện tại");
        }
        jobRotation.setCreatedBy(currentUser.get().getId());

        jobRotation.setCreatedAt(LocalDateTime.now());
        jobRotation.setUpdatedAt(LocalDateTime.now());
        return jobRotationRepository.save(jobRotation);
    }

    // Cập nhật lịch phân công
    @Transactional
    public JobRotation updateJobRotation(Integer id, JobRotation jobRotation) {
        Optional<JobRotation> existingRotation = jobRotationRepository.findById(id);
        if (existingRotation.isEmpty()) {
            throw new IllegalArgumentException("Lịch phân công không tìm thấy");
        }
        validateJobRotation(jobRotation);
        JobRotation rotationToUpdate = existingRotation.get();
        rotationToUpdate.setStaffId(jobRotation.getStaffId());
        rotationToUpdate.setJobPositionId(jobRotation.getJobPositionId());
        rotationToUpdate.setVehicleId(jobRotation.getVehicleId());
        rotationToUpdate.setStatus(jobRotation.getStatus());
        rotationToUpdate.setStartDate(jobRotation.getStartDate());
        rotationToUpdate.setEndDate(jobRotation.getEndDate());
        rotationToUpdate.setUpdatedAt(LocalDateTime.now());
        return jobRotationRepository.save(rotationToUpdate);
    }

    // Xóa lịch phân công
    @Transactional
    public void deleteJobRotation(Integer id) {
        if (!jobRotationRepository.existsById(id)) {
            throw new IllegalArgumentException("Lịch phân công không tìm thấy");
        }
        jobRotationRepository.deleteById(id);
    }

    // Cập nhật trạng thái lịch phân công
    @Transactional
    public JobRotation updateJobRotationStatus(Integer id, String status) {
        Optional<JobRotation> existingRotation = jobRotationRepository.findById(id);
        if (existingRotation.isEmpty()) {
            throw new IllegalArgumentException("Lịch phân công không tìm thấy");
        }
        if (!status.equals("ROTATION_ACTIVE") && !status.equals("ROTATION_INACTIVE")) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: phải là ROTATION_ACTIVE hoặc ROTATION_INACTIVE");
        }
        JobRotation rotation = existingRotation.get();
        rotation.setStatus(status);
        rotation.setUpdatedAt(LocalDateTime.now());
        return jobRotationRepository.save(rotation);
    }

    // Phân công tự động lịch phân công cho tài xế dựa trên tải trọng xe
    @Transactional
    public List<JobRotation> autoAssignJobRotations() {
        List<JobRotation> assignedRotations = new ArrayList<>();

        // Lấy ID nhân viên hiện tại từ SecurityContext
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Staff> currentUser = Optional.ofNullable(staffRepository.findByUserName(username));
        if (currentUser.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin người dùng hiện tại");
        }
        Integer createdBy = currentUser.get().getId();

        // Lấy danh sách tài xế (DRIVER) đang hoạt động
        List<Staff> drivers = staffRepository.findActiveStaffByAuthority("DRIVER", "ACTIVE");
        if (drivers.isEmpty()) {
            throw new IllegalArgumentException("Không có tài xế đang hoạt động");
        }

        // Lấy danh sách vị trí công việc đang hoạt động
        List<JobPosition> activePositions = jobPositionRepository.findByStatus("ACTIVE");
        if (activePositions.isEmpty()) {
            throw new IllegalArgumentException("Không có vị trí công việc đang hoạt động");
        }

        // Phân công công việc cho tài xế
        for (JobPosition position : activePositions) {
            if (position.getRequiredTonnage() == null) {
                continue; // Bỏ qua nếu không có yêu cầu tải trọng
            }

            // Tìm xe phù hợp với tải trọng yêu cầu
            List<Vehicle> suitableVehicles = vehicleRepository.findByStatusAndTonnageGreaterThanEqual(
                    "AVAILABLE", position.getRequiredTonnage());
            if (suitableVehicles.isEmpty()) {
                continue; // Bỏ qua nếu không có xe phù hợp
            }

            // Chọn tài xế ngẫu nhiên (có thể cải tiến bằng thuật toán tối ưu)
            Staff driver = drivers.get((int) (Math.random() * drivers.size()));
            Vehicle vehicle = suitableVehicles.get(0); // Chọn xe đầu tiên phù hợp

            // Tạo lịch phân công
            JobRotation rotation = new JobRotation();
            rotation.setStaffId(driver.getId());
            rotation.setJobPositionId(position.getId());
            rotation.setVehicleId(vehicle.getId());
            rotation.setCreatedBy(createdBy); // Gán người tạo
            rotation.setStatus("ACTIVE");
            rotation.setStartDate(LocalDateTime.now());
            rotation.setCreatedAt(LocalDateTime.now());
            rotation.setUpdatedAt(LocalDateTime.now());

            // Cập nhật trạng thái xe thành IN_USE
            vehicle.setStatus("IN_USE");
            vehicleRepository.save(vehicle);

            assignedRotations.add(jobRotationRepository.save(rotation));
        }

        return assignedRotations;
    }

    // Xác thực dữ liệu lịch phân công
    private void validateJobRotation(JobRotation jobRotation) {
        if (jobRotation.getStaffId() == null || !staffRepository.existsById(jobRotation.getStaffId())) {
            throw new IllegalArgumentException("Nhân viên không hợp lệ");
        }
        if (jobRotation.getJobPositionId() == null || !jobPositionRepository.existsById(jobRotation.getJobPositionId())) {
            throw new IllegalArgumentException("Vị trí công việc không hợp lệ");
        }
        if (jobRotation.getVehicleId() == null || !vehicleRepository.existsById(jobRotation.getVehicleId())) {
            throw new IllegalArgumentException("Xe không hợp lệ");
        }
        if (jobRotation.getCreatedBy() != null && !staffRepository.existsById(jobRotation.getCreatedBy())) {
            throw new IllegalArgumentException("Người tạo lịch phân công không hợp lệ");
        }
        if (jobRotation.getStatus() == null ||
                (!jobRotation.getStatus().equals("ACTIVE") && !jobRotation.getStatus().equals("INACTIVE"))) {
            throw new IllegalArgumentException("Trạng thái phải là ACTIVE hoặc INACTIVE");
        }
        if (jobRotation.getStartDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu là bắt buộc");
        }

        // Kiểm tra tải trọng xe nếu nhân viên là tài xế
        Optional<Staff> staff = staffRepository.findById(jobRotation.getStaffId());
        Optional<JobPosition> position = jobPositionRepository.findById(jobRotation.getJobPositionId());
        Optional<Vehicle> vehicle = vehicleRepository.findById(jobRotation.getVehicleId());
        if (staff.isPresent() && staffRepository.findAuthorityNameByStaffId(staff.get().getId())
                .map(auth -> auth.equals("DRIVER")).orElse(false) &&
                position.isPresent() && vehicle.isPresent() && position.get().getRequiredTonnage() != null) {
            if (vehicle.get().getTonnage().compareTo(position.get().getRequiredTonnage()) < 0) {
                throw new IllegalArgumentException("Tải trọng xe không đáp ứng yêu cầu công việc");
            }
        }
    }
}
