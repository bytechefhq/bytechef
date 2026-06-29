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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Enforces workflow-scope authorization on the automation webhook-trigger-test endpoints before delegating to the
 * shared platform facade. The embedded {@code /internal} controller and the runtime webhook-test path keep calling the
 * shared {@link WebhookTriggerTestFacade} directly and are unaffected.
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
        return webhookTriggerTestFacade.enableTrigger(workflowId, environmentId, PlatformType.AUTOMATION);
    }

    @Override
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')")
    public void disableTrigger(String workflowId, long environmentId) {
        webhookTriggerTestFacade.disableTrigger(workflowId, environmentId, PlatformType.AUTOMATION);
    }
}
