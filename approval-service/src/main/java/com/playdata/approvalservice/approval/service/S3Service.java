package com.playdata.approvalservice.approval.service;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@Slf4j
public class S3Service {

    // application.yml (또는 properties) 파일에 설정된 AWS S3 관련 값들을 주입받습니다.
    @Value("${spring.cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * S3 객체에 대한 임시 접근 URL(Pre-signed URL)을 생성합니다.
     * 이 URL은 썸네일 미리보기(inline) 또는 파일 다운로드(attachment)에 사용됩니다.
     *
     * @param fileKey         S3 버킷 내 파일의 전체 경로 (예: uuid_파일명.png)
     * @param dispositionType 브라우저가 파일을 처리할 방식 ('inline' 또는 'attachment')
     * @return 생성된 Pre-signed URL 문자열
     */
    public String generatePresignedUrl(String fileKey, String dispositionType) {
        log.info("Generating presigned URL for key: {}, type: {}", fileKey, dispositionType);

        // S3 Presigner 객체를 생성합니다. AWS 자격 증명과 리전을 설정합니다.
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                )
                .build();

        try {
            // ★★★ 핵심 수정 부분: 파일 이름을 표준에 맞게 인코딩 ★★★
            // 파일 이름을 UTF-8로 URL 인코딩합니다. Java의 URLEncoder는 공백을 '+'로 바꾸므로,
            // 웹 표준인 '%20'으로 다시 치환해줍니다.
            String encodedFileName = URLEncoder.encode(fileKey, StandardCharsets.UTF_8).replace("+", "%20");

            // RFC 5987 표준에 따라 Content-Disposition 헤더를 구성합니다.
            // 이렇게 하면 한글, 공백, 특수문자가 포함된 파일 이름도 모든 브라우저에서 안전하게 처리됩니다.
            String contentDisposition = dispositionType + "; filename*=UTF-8''" + encodedFileName;

            // S3 객체를 가져오기 위한 기본 요청 객체를 생성합니다.
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey) // S3에 저장된 실제 객체 키는 인코딩되지 않은 원본을 사용합니다.
                    .responseContentDisposition(contentDisposition) // 응답 헤더에만 인코딩된 파일 이름 정보를 포함시킵니다.
                    .build();

            // Pre-signed URL 생성 요청 객체를 만듭니다. URL의 유효 시간은 10분으로 설정합니다.
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .getObjectRequest(getObjectRequest)
                    .build();

            // URL을 생성합니다.
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

            // 사용이 끝난 presigner 리소스를 정리합니다.
            presigner.close();

            // 생성된 URL을 문자열로 반환합니다.
            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Pre-signed URL 생성 중 인코딩 실패. fileKey: {}", fileKey, e);
            // 예외 발생 시 런타임 예외를 던져서 상위 서비스에서 처리하도록 합니다.
            throw new RuntimeException("Pre-signed URL 생성에 실패했습니다.", e);
        }
    }
}