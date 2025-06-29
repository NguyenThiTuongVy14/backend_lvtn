package com.example.test.service;

import com.example.test.entity.Shift;
import com.example.test.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor

public class ShiftService {
    private final ShiftRepository shiftRepository;
    public List<Shift> getShiftsByRole(String role) {
        List<Integer> targetShiftIds = new ArrayList<>();

        if ("DRIVER".equalsIgnoreCase(role)) {
            targetShiftIds = Arrays.asList(2, 4);
        } else if ("COLLECTOR".equalsIgnoreCase(role)) {
            targetShiftIds = Arrays.asList(1, 3);
        } else {
            return new ArrayList<>();
        }

        return shiftRepository.findAllById(targetShiftIds);
    }
}
