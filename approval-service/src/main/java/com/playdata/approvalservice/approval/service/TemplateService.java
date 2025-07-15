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
    private final TemplateCategoryRepository categoryRepository;
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

    /**
     * 템플릿 목록 조회
     * @param templateId
     * @return
     */
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
            if(req.getTemplate() != null) {
                String updatedJson = objectMapper.writeValueAsString(req.getTemplate());
                template.setTemplate(updatedJson);
            }

            if (req.getCategoryId() != null) {
                // 새로 전달된 categoryId로 카테고리 엔티티를 DB에서 찾아옵니다.
                TemplateCategory newCategory = categoryRepository.findById(req.getCategoryId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "변경하려는 카테고리를 찾을 수 없습니다. id=" + req.getCategoryId()));
                template.setCategoryId(newCategory);
                log.info("템플릿 ID {}의 카테고리를 {}로 변경했습니다.", templateId, req.getCategoryId());
            }
            String updatedJson = objectMapper.writeValueAsString(req.getTemplate());
            template.setTemplate(updatedJson);

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
    public List<TemplateResDto> getTemplates(Long categoryId) {
        // 1. Repository에 정의한 메소드를 사용하여 특정 카테고리의 템플릿만 조회
        List<ReportTemplate> templates;

        if (categoryId != null) {
            // categoryId가 제공되면, 해당 카테고리의 템플릿만 조회
            log.info("카테고리 ID로 템플릿 필터링: {}", categoryId);
            templates = templateRepository.findByCategoryId_categoryId(categoryId);
        } else {
            // categoryId가 null이면, 모든 템플릿 조회
            log.info("모든 템플릿 조회");
            templates = templateRepository.findAll();
        }

        // 조회된 템플릿 리스트를 DTO 리스트로 변환
        return templates.stream().map(template -> {
            try {
                // objectMapper는 이미 주입받았으므로 new로 생성할 필요 없습니다.
                return TemplateResDto.from(template, this.objectMapper);
            } catch (JsonProcessingException e) {
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