package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.common.config.AwsS3Config;
import com.playdata.hrservice.hr.entity.Employee;
import com.playdata.hrservice.hr.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final EmployeeRepository employeeRepository;
    private final AwsS3Config awsS3Config;

    public String uploadProfile(Long EmployeeId, MultipartFile file) throws Exception {
        Employee employee = employeeRepository.findByEmployeeId(EmployeeId).orElseThrow(
                () -> new EntityNotFoundException("사용자를 찾을 수 없습니다!")
        );

        // 1) 이전 프로필이 기본 url이 아니고, null도 아니라면 삭제
        String oldUrl = employee.getProfileImageUri();
        if (oldUrl != null && !oldUrl.isBlank()) {
            awsS3Config.deleteFromS3Bucket(oldUrl);
        }

        //2) 새 파일 업로드
        String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String imageUrl = awsS3Config.uploadToS3Bucket(file.getBytes(), uniqueFileName);

        employee.updateProfileImageUri(imageUrl);
        employeeRepository.save(employee);

        return imageUrl;
    }


}
