package com.playdata.hrservice.hr.service;


import com.playdata.hrservice.hr.dto.DepartmentReqDto;
import com.playdata.hrservice.hr.dto.DepartmentResDto;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentResDto getDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Department not found By Id")
        ).toDto();
    }

    public List<DepartmentResDto> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        List<DepartmentResDto> departmentResDtos = departments.stream().map(Department::toDto).toList();
        return departmentResDtos;
    }

    public Department getDepartmentEntity(Long id) {
        return departmentRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Department not found By Id")
        );
    }

    public Department createDepartment(DepartmentReqDto dto) {

        if (departmentRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 부서입니다.");
        }

        Department newDepartment =
                Department.builder()
                         .name(dto.getName())
                         .build();
        departmentRepository.save(newDepartment);

        return newDepartment;
    }
}
