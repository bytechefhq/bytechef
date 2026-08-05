/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.test;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import java.util.List;

/**
 * Mirrors the platform's reserved words so tests can parse an emitted code workflow definition through the real
 * workflow mapper.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TestWorkflowReservedWordContributor implements WorkflowReservedWordContributor {

    @Override
    public List<String> getReservedWords() {
        return List.of(
            "authorizationRequired", "category", "clusterElements", "componentName", "componentVersion", "connections",
            "groupName", "internalOnly", "objectName", "tags", "triggers");
    }
}
