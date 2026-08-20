/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.ee.ai.copilot.web.graphql.facade.PropertyCopilotFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Property Copilot feature.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class PropertyCopilotGraphQlController {

    private final PropertyCopilotFacade propertyCopilotFacade;

    public PropertyCopilotGraphQlController(PropertyCopilotFacade propertyCopilotFacade) {
        this.propertyCopilotFacade = propertyCopilotFacade;
    }

    /**
     * Authorization lives on {@link PropertyCopilotFacade#generatePropertyValue(PropertyCopilotRequest)}, which
     * resolves the owning project from the client-supplied {@code workflowId} and requires {@code WORKFLOW_VIEW} on its
     * workspace &mdash; the API facade is this codebase's authorization layer, and this controller carries no gate of
     * its own. That check used to live in this method's body, where it was invisible to any audit scanning for
     * {@code @PreAuthorize}.
     */
    @MutationMapping
    public GeneratePropertyValuePayload generatePropertyValue(@Argument GeneratePropertyValueInput input) {
        PropertyCopilotResult result = propertyCopilotFacade.generatePropertyValue(new PropertyCopilotRequest(
            input.prompt(), input.mode(), input.workflowId(), input.workflowNodeName(), input.propertyPath(),
            input.propertyType(), input.dynamic(), input.environmentId()));

        return new GeneratePropertyValuePayload(result.value(), result.valid(), result.message());
    }

    @SuppressFBWarnings("EI")
    public record GeneratePropertyValueInput(
        String prompt, PropertyCopilotMode mode, String workflowId, String workflowNodeName, String propertyPath,
        String propertyType, boolean dynamic, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GeneratePropertyValuePayload(String value, boolean valid, String message) {
    }
}
