package com.playdata.noticeservice.notice.service;

import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.playdata.noticeservice.common.config.AwsS3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {
    // S3 버킷을 제어하는 객체
    private S3Client s3Client;

    @Value("${spring.cloud.aws.credentials.accessKey}")
    private String accessKey;
    @Value("${spring.cloud.aws.credentials.secretKey}")
    private String secretKey;
    @Value("${spring.cloud.aws.region.static}")
    private String region;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;


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

    public void deleteFiles(List<String> attachmentUri) {
        for (String url : attachmentUri) {
            try {
                awsS3Config.deleteFromS3Bucket(url); // ✅ 변경
            } catch (Exception e) {
                e.printStackTrace(); // 실제 서비스라면 로그 처리 권장
            }
        }
    }

    public String generatePresignedUrlForPut(String fileName, String contentType) {
        log.info("contentType: {}", contentType);
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                )
                .build();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileType", contentType);
        metadata.put("contentType", contentType);


        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)  // ✅ 여기 추가
                .metadata(metadata)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(objectRequest)
                .signatureDuration(java.time.Duration.ofMinutes(5))
                .build();

        String url = presigner.presignPutObject(presignRequest).url().toString();
        return url;
    }

    public String generatePresignedUrlForGet(String fileName, String contentType) {
        log.info("contentType: {}", contentType);
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                )
                .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        presigner.close();

        return presignedRequest.url().toString();

    }


}
