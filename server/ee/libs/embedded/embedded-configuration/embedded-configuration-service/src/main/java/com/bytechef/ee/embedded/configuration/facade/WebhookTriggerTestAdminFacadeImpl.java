/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class WebhookTriggerTestAdminFacadeImpl implements WebhookTriggerTestAdminFacade {

    private static final String DEVELOPMENT = "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT";

    private final WebhookTriggerTestFacade webhookTriggerTestFacade;

    @SuppressFBWarnings("EI")
    public WebhookTriggerTestAdminFacadeImpl(WebhookTriggerTestFacade webhookTriggerTestFacade) {
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
    }

    @Override
    @PreAuthorize("hasWorkflowScopeInEnvironment(#workflowId, 'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public String startWebhookTriggerTest(String workflowId, @Nullable String triggerName, long environmentId) {
        return webhookTriggerTestFacade.enableTrigger(workflowId, triggerName, environmentId, PlatformType.EMBEDDED);
    }

    @Override
    @PreAuthorize("hasWorkflowScopeInEnvironment(#workflowId, 'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void stopWebhookTriggerTest(String workflowId, @Nullable String triggerName, long environmentId) {
        webhookTriggerTestFacade.disableTrigger(workflowId, triggerName, environmentId, PlatformType.EMBEDDED);
    }
}
