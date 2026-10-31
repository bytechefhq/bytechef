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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Starting or stopping a webhook trigger test registers or removes a live webhook for the workflow, so it needs
 * {@code WORKFLOW_EDIT} in the environment the test runs in. The checks live here rather than on
 * {@link WebhookTriggerTestFacade}, which the anonymous webhook receive path also calls.
 *
 * @author Ivica Cardic
 */
@Service
public class ProjectWebhookTriggerTestFacadeImpl implements ProjectWebhookTriggerTestFacade {

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;

    @SuppressFBWarnings("EI")
    public ProjectWebhookTriggerTestFacadeImpl(WebhookTriggerTestFacade webhookTriggerTestFacade) {
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_EDIT', #environmentId)")
    public String startWebhookTriggerTest(String workflowId, @Nullable String triggerName, long environmentId) {
        return webhookTriggerTestFacade.enableTrigger(workflowId, triggerName, environmentId, PlatformType.AUTOMATION);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_EDIT', #environmentId)")
    public void stopWebhookTriggerTest(String workflowId, @Nullable String triggerName, long environmentId) {
        webhookTriggerTestFacade.disableTrigger(workflowId, triggerName, environmentId, PlatformType.AUTOMATION);
    }
}
