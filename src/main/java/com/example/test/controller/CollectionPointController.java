package com.example.test.controller;

import com.example.test.repository.StaffRepository;
import com.example.test.service.CollectionPointService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/collection-points")
public class CollectionPointController {

    private final CollectionPointService collectionPointService;
    private final StaffRepository staffRepository;

    @Autowired
    public CollectionPointController(CollectionPointService collectionPointService,
                                     StaffRepository staffRepository) {
        this.collectionPointService = collectionPointService;
        this.staffRepository = staffRepository;
    }

}