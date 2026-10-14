/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectCodeWorkflowDeployResultModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectCodeWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectCodeWorkflowTemplateModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * API-key counterpart of the internal embedded automation code workflow deploy endpoint, authenticated as the key's own
 * ByteChef user by {@code EmbeddedPlatformUserApiKeySecurityConfigurer}. The models carry no automation project id.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.AutomationProjectCodeWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationProjectCodeWorkflowApiController implements AutomationProjectCodeWorkflowApi {

    private final AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationProjectCodeWorkflowApiController(
        AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade,
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {

        this.automationWorkflowProjectCodeWorkflowFacade = automationWorkflowProjectCodeWorkflowFacade;
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @Override
    public ResponseEntity<AutomationProjectCodeWorkflowDeployResultModel> deployAutomationProjectCodeWorkflow(
        EnvironmentModel xEnvironment, MultipartFile projectFile) {

        List<String> warnings;

        try {
            warnings = automationWorkflowProjectCodeWorkflowFacade.save(
                projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(new AutomationProjectCodeWorkflowDeployResultModel().warnings(warnings));
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<List<AutomationProjectCodeWorkflowModel>> listAutomationProjectCodeWorkflows() {
        List<AutomationProjectCodeWorkflowModel> automationProjectCodeWorkflowModels =
            automationWorkflowProjectFacade.getPublishedProjects()
                .stream()
                .map(this::toModel)
                .toList();

        return ResponseEntity.ok(automationProjectCodeWorkflowModels);
    }

    private AutomationProjectCodeWorkflowModel toModel(AutomationWorkflowProjectDTO automationWorkflowProjectDTO) {
        List<AutomationProjectCodeWorkflowTemplateModel> workflowTemplateModels =
            automationWorkflowProjectDTO.workflowTemplates()
                .stream()
                .map(workflowTemplate -> new AutomationProjectCodeWorkflowTemplateModel()
                    .label(workflowTemplate.label()))
                .toList();

        return new AutomationProjectCodeWorkflowModel()
            .name(automationWorkflowProjectDTO.name())
            .kind(
                automationWorkflowProjectDTO.codeWorkflowProject()
                    ? AutomationProjectCodeWorkflowModel.KindEnum.REFERENCE
                    : AutomationProjectCodeWorkflowModel.KindEnum.COPY)
            .workflowTemplates(workflowTemplateModels);
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }
}
