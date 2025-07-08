package com.playdata.approvalservice.approval.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.approvalservice.approval.dto.request.template.TemplateCreateReqDto;
import com.playdata.approvalservice.approval.dto.request.template.TemplateUpdateReqDto;
import com.playdata.approvalservice.approval.dto.response.template.TemplateResDto;
import com.playdata.approvalservice.approval.entity.ReportTemplate;
import com.playdata.approvalservice.approval.repository.ReportTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateService {

    private final ReportTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TemplateResDto createTemplate(TemplateCreateReqDto req) {
        try {
            ReportTemplate newTemplate = ReportTemplate.of(req, objectMapper);
            ReportTemplate savedTemplate = templateRepository.save(newTemplate);
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
}