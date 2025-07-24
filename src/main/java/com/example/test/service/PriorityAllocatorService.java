package com.example.test.service;

import com.example.test.entity.RotationLog;
import com.example.test.repository.RotationLogRepository;
import com.example.test.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriorityAllocatorService {

    private final RotationLogRepository rotationLogRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * Đẩy các WAITLIST (ưu tiên carry_points cao) lên ASSIGNED nếu còn slot.
     */
    @Transactional
    public void promoteWaitlistIfAny(LocalDate date) {
        int capacity = (int) vehicleRepository.count();
        long assigned = rotationLogRepository.countAssignedForUpdate(date);
        int remain = capacity - (int) assigned;
        if (remain <= 0) return;

        List<RotationLog> waitlist = rotationLogRepository.findWaitlistOrderByCarryPointsForUpdate(date);
        LocalDateTime now = LocalDateTime.now();

        for (RotationLog r : waitlist) {
            if (remain == 0) break;
            r.setStatus("ASSIGNED");
            r.setUpdatedAt(now);
            remain--;
        }
        rotationLogRepository.saveAll(waitlist);
    }
}
