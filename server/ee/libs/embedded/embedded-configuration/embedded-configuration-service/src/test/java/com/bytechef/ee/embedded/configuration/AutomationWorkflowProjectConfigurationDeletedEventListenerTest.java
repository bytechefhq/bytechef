/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceRolloutManager;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectConfigurationDeletedEventListener;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectConfigurationDeletedEventListenerTest {

    @Test
    void testDeletedEventHandsEveryProvidedInputNameToTheSerializedRollout() {
        ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager = mock(
            ConnectedUserReferenceRolloutManager.class);

        AutomationWorkflowProjectConfigurationDeletedEventListener listener =
            new AutomationWorkflowProjectConfigurationDeletedEventListener(connectedUserReferenceRolloutManager);

        listener.onAutomationWorkflowProjectConfigurationDeleted(
            new AutomationWorkflowProjectConfigurationDeletedEvent(
                4004L, 2L, Map.of("workflow-uuid", Set.of("channel"))));

        verify(connectedUserReferenceRolloutManager).rollOutAndApply(
            4004L, 2L, Map.of("workflow-uuid", Set.of("channel")));
    }
}
