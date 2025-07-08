package com.playdata.noticeservice.notice.service;

import com.playdata.noticeservice.common.config.AwsS3Config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AwsS3Config awsS3Config; // ✅ 기존 AmazonS3 → AwsS3Config 주입

    public String uploadFile(MultipartFile file, String dir) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일입니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String fileName = dir + "/" + uuid + "_" + originalFilename;

        byte[] fileBytes = file.getBytes();
        return awsS3Config.uploadToS3Bucket(fileBytes, fileName); // 업로드 후 URL 반환
    }

    public List<String> uploadFiles(List<MultipartFile> files, String dir) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                urls.add(uploadFile(file, dir));
            }
        }
        return urls;
    }


    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                String uuid = UUID.randomUUID().toString();
                String fileName = uuid + "_" + originalFilename;

                byte[] fileBytes = file.getBytes();
                String url = awsS3Config.uploadToS3Bucket(fileBytes, fileName); // ✅ 변경
                urls.add(url);
            }
        }
        return urls;
    }

    public void deleteFiles(List<String> fileUrls) {
        for (String url : fileUrls) {
            try {
                awsS3Config.deleteFromS3Bucket(url); // ✅ 변경
            } catch (Exception e) {
                e.printStackTrace(); // 실제 서비스라면 로그 처리 권장
            }
        }
    }
}
