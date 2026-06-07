/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.test;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import java.util.List;

/**
 * Test-only stand-in for the production
 * {@code com.bytechef.platform.configuration.workflow.contributor.WorkflowReservedWordContributorImpl}. Mirrors the
 * platform-configuration-service contributor's word list so {@link com.bytechef.atlas.configuration.domain.Workflow}'s
 * validator accepts {@code triggers} / {@code clusterElements} / etc. without dragging the full
 * platform-configuration-service module into the context-store test classpath.
 *
 * <p>
 * Registered via {@code META-INF/services/com.bytechef.atlas.configuration.workflow.contributor
 * .WorkflowReservedWordContributor} under {@code src/test/resources}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class TestWorkflowReservedWordContributor implements WorkflowReservedWordContributor {

    @Override
    public List<String> getReservedWords() {
        return List.of(
            "authorizationRequired", "category", "clusterElements", "componentName", "componentVersion", "connections",
            "tags", "triggers");
    }
}
