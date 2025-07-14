package com.playdata.approvalservice.approval.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.dto.request.template.TemplateCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.template.TemplateUpdateReqDto;
import com.playdata.approvalservice.approval.dto.response.template.TemplateResDto;
import com.playdata.approvalservice.approval.entity.ReportTemplate;
import com.playdata.approvalservice.approval.entity.TemplateCategory;
import com.playdata.approvalservice.approval.repository.ReportTemplateRepository;
import com.playdata.approvalservice.approval.repository.TemplateCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateService {

    private final ReportTemplateRepository templateRepository;
    private TemplateCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 템플릿 생성
     * @param req
     * @return
     */
    @Transactional
    public TemplateResDto createTemplate(TemplateCreateReqDto req) {

        TemplateCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다. id=" + req.getCategoryId()));
        try {
            // 1. DTO에서 JsonNode를 가져와서 명시적으로 String으로 변환
            String jsonTemplate = objectMapper.writeValueAsString(req.getTemplate());

            // 2. Builder를 사용하여 직접 엔티티 생성
            ReportTemplate newTemplate = ReportTemplate.builder()
                    .template(jsonTemplate)
                    .categoryId(category)
                    .build();

            // 3. 엔티티 저장
            ReportTemplate savedTemplate = templateRepository.save(newTemplate);

            // 4. 응답 DTO로 변환하여 반환
            return TemplateResDto.from(savedTemplate, objectMapper);

        } catch (JsonProcessingException e) {
            log.error("템플릿 JSON 직렬화/파싱 실패 (생성)", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 템플릿 JSON 형식입니다.", e);
        }
    }

    public TemplateResDto getTemplate(Long templateId) {
        ReportTemplate template = findTemplateById(templateId);
        try {
            return TemplateResDto.from(template, objectMapper);
        } catch (JsonProcessingException e) {
            log.error("템플릿 JSON 파싱 실패 (조회): templateId={}", templateId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "저장된 템플릿 데이터 파싱에 실패했습니다.", e);
        }
    }

    @Transactional
    public TemplateResDto updateTemplate(Long templateId, TemplateUpdateReqDto req) {
        ReportTemplate template = findTemplateById(templateId);
        try {
            String updatedJson = objectMapper.writeValueAsString(req.getTemplate());
            template.setTemplate(updatedJson);
            // templateRepository.save(template)는 Transactional 환경에서 생략 가능 (더티 체킹)
            return TemplateResDto.from(template, objectMapper);
        } catch (JsonProcessingException e) {
            log.error("템플릿 JSON 직렬화/파싱 실패 (수정): templateId={}", templateId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 템플릿 JSON 형식입니다.", e);
        }
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        ReportTemplate template = findTemplateById(templateId);
        templateRepository.delete(template);
    }

    /**
     * ID로 템플릿을 찾는 private 헬퍼 메소드
     */
    private ReportTemplate findTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다. id=" + templateId));
    }

    /**
     * 특정 카테고리에 속한 모든 템플릿 조회
     */
    public List<TemplateResDto> getTemplatesByCategoryId(Long categoryId) {
        // 1. Repository에 정의한 메소드를 사용하여 특정 카테고리의 템플릿만 조회
        List<ReportTemplate> templates = templateRepository.findByCategoryId(categoryId);

        // 2. 조회된 템플릿 리스트를 DTO 리스트로 변환
        return templates.stream().map(template -> {
            try {
                return TemplateResDto.from(template, objectMapper);
            } catch (JsonProcessingException e) {
                // 이 경우, DB에 저장된 JSON이 잘못된 형식일 가능성이 높음
                log.error("DB의 템플릿 JSON 파싱 실패: templateId={}", template.getTemplateId(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "저장된 템플릿 데이터 파싱 오류");
            }
        }).collect(Collectors.toList());
    }


    /**
     * 모든 템플릿 조회
     * @return
     */
    public List<TemplateResDto> getAllTemplates() {
        List<ReportTemplate> templates = templateRepository.findAll();
        return templates.stream().map(
                reportTemplate -> {
                    try {
                        return TemplateResDto.from(reportTemplate, new ObjectMapper());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).toList();

    }
}