/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.web.rest.model.StartWebhookTriggerTest200ResponseModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * These endpoints are embedded-<em>typed</em>, not merely embedded-<em>hosted</em>, and that -- not the
 * {@code /api/embedded/internal} path they sit on -- is what decides who may call them. Both hardcode
 * {@link PlatformType#EMBEDDED} when calling the shared facade, which resolves the workflow through
 * {@code IntegrationJobPrincipalAccessor}: {@code WebhookTriggerTestFacadeImpl#executeTrigger} opens with
 * {@code getWorkflowUuid(workflowId, type)}, reaching
 * {@code IntegrationWorkflowServiceImpl#getWorkflowIntegrationWorkflow}, which is
 * {@code findByWorkflowId(workflowId).orElseThrow(...)}. A workflow with no {@code integration_workflow} row does not
 * fail some later step; it fails the FIRST statement, with
 * {@code IllegalArgumentException("Workflow not found for id: ...")} -- a 400.
 *
 * <p>
 * So the only caller is {@code Integration.tsx}, the admin console's integration editor, whose workflows are created by
 * {@code IntegrationWorkflowFacadeImpl#addWorkflow} -> {@code IntegrationWorkflowService#addWorkflow} and therefore do
 * have that row.
 *
 * <p>
 * <strong>Do not repoint the other two embedded editor pages here.</strong> {@code WorkflowBuilder.tsx} (the connected
 * user's embedded builder) and {@code AutomationWorkflow.tsx} (the admin console's automation-workflow-project editor)
 * live under {@code client/src/ee/pages/embedded} and look like they belong, but both edit PROJECT workflows --
 * provisioned via {@code projectWorkflowFacade.addWorkflow} in {@code AutomationWorkflowProjectFacadeImpl} and the
 * connected-user copy/reference paths -- which have a {@code project_workflow} row and no {@code integration_workflow}
 * one. Sending them here 400s on every call. They use the automation endpoint, and correctly so; the same rule holds
 * elsewhere in the embedded tree, e.g. the connected-user bridge dispatches those workflows as
 * {@code PlatformType.AUTOMATION} (see {@code .agents/embedded-bridge.md}). The endpoint follows the WORKFLOW KIND, not
 * the page's directory.
 *
 * <p>
 * That settles the gate. The reachable population is a tenant admin on an integration workflow, so
 * {@code isTenantAdmin()} is not a placeholder for a finer check -- it is the check. The automation twin's
 * {@code hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')} ({@code WebhookTriggerTestApiFacadeImpl:57,68}) could
 * only ever pass here through its own {@code isTenantAdmin()} step anyway: walking
 * {@code PermissionServiceImpl.hasResourceScope} in the order it runs, the {@code ResourceMembershipDecider} stage
 * answers only for an embedded connected user, who cannot reach this endpoint; and the ownership stage resolves
 * {@code 'Workflow'} through {@code WorkflowOwnershipResolver} -> {@code ProjectRepository.findByWorkflowId}, which an
 * integration workflow has no row for, so it answers {@code ResourceOwner.unknown()}. A scope check that no amount of
 * workspace scope can satisfy is the kind of gate that later reads as working RBAC and is not. {@code isTenantAdmin()}
 * states the same outcome truthfully and matches {@code IntegrationWorkflowFacadeImpl}, which owns these very workflow
 * ids and carries it on every method -- and is the only way to open this editor at all.
 *
 * <p>
 * Substituting the twin's expression here would also be a LOOSENING, not a tightening: it newly admits an ordinary
 * non-admin workspace member holding {@code WORKFLOW_EDIT}. That expression is environment-agnostic, while the
 * {@code environmentId} below is caller-supplied, so such a member who is an editor in Development could pass
 * {@code environmentId} naming PRODUCTION and mint a live webhook URL there. {@code isTenantAdmin()} closes that by
 * closing the population.
 *
 * <p>
 * One divergence between the two expressions is worth naming so nobody later "simplifies" one into the other: for an
 * embedded connected user the twin is answered by {@code ResourceMembershipDecider}, which GRANTS the caller's own
 * workflow, where this gate DENIES — a connected user is never a tenant admin. Unreachable from here, since this
 * controller is admin-only by construction, but it is the reason the two expressions are not interchangeable.
 *
 * <p>
 * The gate sits on the controller rather than on a new per-controller facade (the pattern the automation twin uses)
 * because there is no shared domain surface to protect here: the shared {@code WebhookTriggerTestFacade} must stay
 * ungated for the automation caller and for the runtime auto-disable in {@code WorkflowNodeTestOutputFacadeImpl}, this
 * controller is the only embedded caller, and a principal-level check needs no collaborator. Keeping it here also keeps
 * it beside {@code resolveRequiredEnvironmentId} below, the other half of this endpoint pair's security.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.web.rest.WebhookTriggerTestApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class WebhookTriggerTestApiController implements WebhookTriggerTestApi {

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;

    public WebhookTriggerTestApiController(WebhookTriggerTestFacade webhookTriggerTestFacade) {
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public ResponseEntity<StartWebhookTriggerTest200ResponseModel> startWebhookTriggerTest(
        String workflowId, Long environmentId) {

        // isTenantAdmin() above carries no environment, so the environmentId reaching enableTrigger must still be
        // resolved to the caller's own: it mints a live webhook URL, runs executeWebhookEnable against a
        // test-configuration connection, and the resulting callback writes a test output, all in whatever
        // environment this value names. See PrincipalEnvironment.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        String webhookUrl = webhookTriggerTestFacade.enableTrigger(
            workflowId, effectiveEnvironmentId, PlatformType.EMBEDDED);

        return ResponseEntity.ok(
            new StartWebhookTriggerTest200ResponseModel()
                .webhookUrl(webhookUrl));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public ResponseEntity<Void> stopWebhookTriggerTest(String workflowId, Long environmentId) {
        // Same as startWebhookTriggerTest above. See PrincipalEnvironment.
        long effectiveEnvironmentId = resolveRequiredEnvironmentId(environmentId);

        webhookTriggerTestFacade.disableTrigger(workflowId, effectiveEnvironmentId, PlatformType.EMBEDDED);

        return ResponseEntity.noContent()
            .build();
    }

    // Required by the OpenAPI contract (@NotNull, required = true) on both callers, so Spring rejects a missing
    // environmentId before either runs -- checked explicitly all the same, because the alternative is an unboxing
    // NPE surfacing as a 500 if that ever changes.
    private static long resolveRequiredEnvironmentId(Long environmentId) {
        if (environmentId == null) {
            throw new IllegalArgumentException("environmentId is required");
        }

        return PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId.longValue());
    }
}
