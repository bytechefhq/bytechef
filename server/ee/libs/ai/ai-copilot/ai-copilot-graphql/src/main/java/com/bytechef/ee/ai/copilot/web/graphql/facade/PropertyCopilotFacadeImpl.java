/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.ai.copilot.CopilotGenerationException;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotGenerator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
class PropertyCopilotFacadeImpl implements PropertyCopilotFacade {

    private final PropertyCopilotGenerator propertyCopilotGenerator;

    PropertyCopilotFacadeImpl(Optional<PropertyCopilotGenerator> propertyCopilotGenerator) {
        this.propertyCopilotGenerator = propertyCopilotGenerator.orElse(null);
    }

    @Override
    @PreAuthorize("hasPermission(#request.workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public PropertyCopilotResult generatePropertyValue(PropertyCopilotRequest request) {
        if (propertyCopilotGenerator == null) {
            throw new IllegalStateException("Property Copilot is not enabled");
        }

        try {
            return propertyCopilotGenerator.generate(request);
        } catch (ChatModelUnavailableException | CopilotGenerationException exception) {
            throw new GraphQlBadRequestException(exception.getMessage(), exception);
        }
    }
}
