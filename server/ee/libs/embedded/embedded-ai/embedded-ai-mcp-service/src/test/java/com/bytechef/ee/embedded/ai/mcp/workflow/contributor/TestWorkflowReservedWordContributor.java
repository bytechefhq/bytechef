/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.workflow.contributor;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TestWorkflowReservedWordContributor implements WorkflowReservedWordContributor {

    @Override
    public List<String> getReservedWords() {
        return List.of("triggers");
    }
}
