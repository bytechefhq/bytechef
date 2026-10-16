/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.listener;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationBeforeDeleteEventListener extends AbstractRelationalEventListener<Integration> {

    private final IntegrationCodeWorkflowService integrationCodeWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationBeforeDeleteEventListener(IntegrationCodeWorkflowService integrationCodeWorkflowService) {
        this.integrationCodeWorkflowService = integrationCodeWorkflowService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<Integration> event) {
        Identifier identifier = event.getId();

        integrationCodeWorkflowService.deleteIntegrationCodeWorkflows((Long) identifier.getValue());
    }
}
