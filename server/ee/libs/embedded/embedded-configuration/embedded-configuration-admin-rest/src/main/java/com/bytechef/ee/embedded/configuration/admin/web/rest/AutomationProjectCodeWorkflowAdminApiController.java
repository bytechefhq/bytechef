/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.admin.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.admin.web.rest.model.AutomationProjectCodeWorkflowDeployResultModel;
import com.bytechef.ee.embedded.configuration.admin.web.rest.model.AutomationWorkflowProjectModel;
import com.bytechef.ee.embedded.configuration.admin.web.rest.model.AutomationWorkflowProjectWorkflowTemplateModel;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin-API counterpart of {@code AutomationProjectCodeWorkflowApiController} (embedded internal). That internal
 * endpoint is reachable only through {@code EmbeddedApiKeySecurityConfigurer}'s connected-user auth, which requires a
 * {@code /v<n>/{externalUserId}/} path segment and grants zero authorities -- a plain API-key bearer token can never
 * satisfy the facade's {@code ROLE_ADMIN} guard through it.
 *
 * <p>
 * This controller is mounted on {@code /api/embedded/v1/**} alongside the rest of the embedded API, and carved out of
 * that connected-user configurer by {@code EmbeddedAdminApiKeySecurityConfigurer}, whose {@code PATH_PATTERN} the two
 * share. That configurer authenticates the bearer token as the key's own ByteChef user with their real Spring
 * authorities, and accepts admin API keys only -- these operations act on the whole tenant rather than on one workspace
 * or environment.
 *
 * <p>
 * Deploying through here creates the same embedded relation as the internal endpoint: the resulting catalog
 * {@code Project} is resolved/created through {@code AutomationWorkflowProjectFacade}'s marker convention, so the
 * automation project stays hidden behind the embedded automation-workflow-project entity. Never expose {@code Project}
 * ids on this surface -- only embedded-entity identifiers are meant to reach embedded callers.
 *
 * <p>
 * Authorization note: the {@code ROLE_ADMIN} guard lives on
 * {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl#save}, so it is enforced identically regardless of which
 * controller reaches the facade. The internal endpoint keeps working unchanged for the admin console (browser session);
 * this endpoint exists in addition to it, for token-authenticated callers such as the CLI.
 *
 * <p>
 * {@link #listAutomationProjectCodeWorkflows()} deliberately does not mirror
 * {@code AutomationWorkflowProjectApiController#getFrontendProjects} (embedded public-rest): that endpoint is matched
 * by {@code EmbeddedApiKeySecurityConfigurer}'s connected-user auth, whose externalUserId regex incidentally captures
 * the literal path segment {@code "automation"} for a no-externalUserId path and silently creates a phantom
 * {@code ConnectedUser} row per tenant/environment as a side effect. Listing here instead reuses
 * {@link AutomationWorkflowProjectFacade#getPublishedProjects()} directly, with no connected-user identity involved.
 * That facade method itself carries no {@code @PreAuthorize} -- it also backs the public connected-user catalog
 * listing, so it cannot be gated at the shared layer. The {@code ROLE_ADMIN} guard is therefore applied directly on
 * this method, mirroring the {@code hasAuthority(ADMIN)} guard already enforced on
 * {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl#save} for the sibling deploy endpoint.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.admin.web.rest.AutomationProjectCodeWorkflowAdminApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationProjectCodeWorkflowAdminApiController implements AutomationProjectCodeWorkflowAdminApi {

    private final AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationProjectCodeWorkflowAdminApiController(
        AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade,
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {

        this.automationWorkflowProjectCodeWorkflowFacade = automationWorkflowProjectCodeWorkflowFacade;
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @Override
    public ResponseEntity<AutomationProjectCodeWorkflowDeployResultModel> deployAutomationProjectCodeWorkflow(
        MultipartFile projectFile) {

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
    public ResponseEntity<List<AutomationWorkflowProjectModel>> listAutomationProjectCodeWorkflows() {
        List<AutomationWorkflowProjectModel> models = automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .map(this::toModel)
            .toList();

        return ResponseEntity.ok(models);
    }

    private AutomationWorkflowProjectModel toModel(AutomationWorkflowProjectDTO project) {
        List<AutomationWorkflowProjectWorkflowTemplateModel> workflowTemplateModels = project.workflowTemplates()
            .stream()
            .map(workflowTemplate -> new AutomationWorkflowProjectWorkflowTemplateModel()
                .label(workflowTemplate.label()))
            .toList();

        return new AutomationWorkflowProjectModel()
            .name(project.name())
            .kind(
                project.codeWorkflowProject()
                    ? AutomationWorkflowProjectModel.KindEnum.REFERENCE
                    : AutomationWorkflowProjectModel.KindEnum.COPY)
            .workflowTemplates(workflowTemplateModels);
    }
}
