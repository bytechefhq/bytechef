/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.facade;

import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Enforces workflow-scope authorization on the automation webhook-trigger-test endpoints before delegating to the
 * shared platform facade.
 *
 * <p>
 * {@code enableTrigger}/{@code disableTrigger} on the shared {@link WebhookTriggerTestFacade} have two other callers,
 * and the environment axis is NOT the same story for each:
 * <ul>
 * <li>The embedded {@code /internal} controller
 * ({@code com.bytechef.ee.embedded.configuration.web.rest.WebhookTriggerTestApiController}) also calls these two
 * methods directly. It resolves {@code environmentId} itself ({@link PrincipalEnvironment}) before calling in, the same
 * fix applied here, not "unaffected"; and it carries its own authorization gate too -- {@code isTenantAdmin()}, on the
 * controller rather than on a facade, and deliberately NOT this class's expression. Two reasons, and the first is the
 * one that also decides which client pages may call it at all: that controller hardcodes {@code PlatformType.EMBEDDED},
 * which resolves the workflow through {@code IntegrationJobPrincipalAccessor} and so requires an
 * {@code integration_workflow} row -- its only caller is the admin console's integration editor, a tenant admin on an
 * integration workflow. Second, for exactly that workflow kind a {@code 'Workflow'} scope check cannot be satisfied on
 * its merits (no {@code project_workflow} row for {@code WorkflowOwnershipResolver} to resolve), so this class's
 * expression could only ever pass there via its own {@code isTenantAdmin()} step, while newly admitting an ordinary
 * non-admin member -- a loosening, not a tightening. That class's javadoc has the full reasoning and is the authority;
 * this entry exists so the inventory of who reaches the shared facade stays complete.
 * {@code WebhookTriggerTestApiControllerAuthorizationTest} pins the expression this sentence claims, and
 * {@code WebhookTriggerTestApiControllerTest} pins the {@code PlatformType} the first reason turns on, so neither can
 * drift silently.</li>
 * <li>The runtime webhook-test path ({@code WebhookTriggerTestController}) genuinely IS unaffected: it calls different
 * methods on the shared facade ({@code getWebhookTriggerFlags}, {@code isWorkflowEnabled}, {@code validateOnEnable}),
 * none of which take a caller-supplied {@code environmentId} -- they're driven by a {@code WorkflowExecutionId}, which
 * already carries the environment the job actually ran in.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
@Service
class WebhookTriggerTestApiFacadeImpl implements WebhookTriggerTestApiFacade {

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;

    @SuppressFBWarnings("EI")
    WebhookTriggerTestApiFacadeImpl(WebhookTriggerTestFacade webhookTriggerTestFacade) {
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
    }

    @Override
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')")
    public String enableTrigger(String workflowId, long environmentId) {
        // hasPermission(#workflowId, 'Workflow', ...) above is environment-agnostic, so the caller-supplied
        // environmentId is never checked -- and this mints a live webhook URL in whatever environment it names.
        // See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        return webhookTriggerTestFacade.enableTrigger(workflowId, effectiveEnvironmentId, PlatformType.AUTOMATION);
    }

    @Override
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')")
    public void disableTrigger(String workflowId, long environmentId) {
        // See enableTrigger above. See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        webhookTriggerTestFacade.disableTrigger(workflowId, effectiveEnvironmentId, PlatformType.AUTOMATION);
    }
}
