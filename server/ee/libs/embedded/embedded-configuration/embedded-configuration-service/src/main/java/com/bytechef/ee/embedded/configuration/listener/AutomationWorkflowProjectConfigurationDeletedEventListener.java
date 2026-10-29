/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.listener;

import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceRolloutManager;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AutomationWorkflowProjectConfigurationDeletedEventListener {

    private final ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationDeletedEventListener(
        ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager) {

        this.connectedUserReferenceRolloutManager = connectedUserReferenceRolloutManager;
    }

    @Async("workerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAutomationWorkflowProjectConfigurationDeleted(
        AutomationWorkflowProjectConfigurationDeletedEvent automationWorkflowProjectConfigurationDeletedEvent) {

        connectedUserReferenceRolloutManager.rollOutAndApply(
            automationWorkflowProjectConfigurationDeletedEvent.projectId(),
            automationWorkflowProjectConfigurationDeletedEvent.environmentId(),
            automationWorkflowProjectConfigurationDeletedEvent.removedProvidedInputNames());
    }
}
