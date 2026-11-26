/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.deployment;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.ByteArrayResource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkflowDefinitions {

    private WorkflowDefinitions() {
    }

    public static boolean sameDefinition(Workflow left, Workflow right) {
        return sameDefinition(left.getDefinition(), left.getFormat(), right.getDefinition(), right.getFormat());
    }

    public static boolean sameDefinition(String left, Format leftFormat, String right, Format rightFormat) {
        if (leftFormat == rightFormat && Objects.equals(left, right)) {
            return true;
        }

        Map<String, Object> leftMap = readDefinition(left, leftFormat);
        Map<String, Object> rightMap = readDefinition(right, rightFormat);

        return leftMap.equals(rightMap);
    }

    private static Map<String, Object> readDefinition(String definition, Format format) {
        WorkflowResource workflowResource = new WorkflowResource(
            null, Map.of(), new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)), format);

        try {
            return WorkflowReader.readWorkflowMap(workflowResource);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Workflow definition is not valid " + format, exception);
        }
    }
}
