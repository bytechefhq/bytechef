/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;

/**
 * Facade for the {@code generatePropertyValue} GraphQL mutation. Hosts the {@code WORKFLOW_VIEW} authorization guard on
 * the client-supplied {@code workflowId} so it applies to every caller of the facade rather than only the GraphQL entry
 * point, and keeps it off the shared {@code PropertyCopilotGenerator}, which is a prompt-building collaborator rather
 * than an authorization boundary. The facade lives in the GraphQL module because it exists purely to give this one
 * mutation an authorization layer.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface PropertyCopilotFacade {

    PropertyCopilotResult generatePropertyValue(PropertyCopilotRequest request);
}
