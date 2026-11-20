/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class CarriedInputUtils {

    private CarriedInputUtils() {
    }

    public static Map<String, Object> retainInputsOfUnchangedType(
        Workflow sourceWorkflow, Workflow targetWorkflow, Map<String, ?> inputs) {

        Map<String, WorkflowInput> sourceWorkflowInputs = getWorkflowInputsByName(sourceWorkflow);
        Map<String, WorkflowInput> targetWorkflowInputs = getWorkflowInputsByName(targetWorkflow);

        Map<String, Object> carriedInputs = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : inputs.entrySet()) {
            WorkflowInput sourceWorkflowInput = sourceWorkflowInputs.get(entry.getKey());
            WorkflowInput targetWorkflowInput = targetWorkflowInputs.get(entry.getKey());

            if (sourceWorkflowInput == null || targetWorkflowInput == null) {
                continue;
            }

            if (StringUtils.equalsIgnoreCase(sourceWorkflowInput.getType(), targetWorkflowInput.getType())) {
                carriedInputs.put(entry.getKey(), entry.getValue());
            }
        }

        return carriedInputs;
    }

    private static Map<String, WorkflowInput> getWorkflowInputsByName(Workflow workflow) {
        return WorkflowInput.of(workflow)
            .stream()
            .collect(
                Collectors.toMap(
                    WorkflowInput::getName, Function.identity(), (workflowInput, otherWorkflowInput) -> workflowInput));
    }
}
