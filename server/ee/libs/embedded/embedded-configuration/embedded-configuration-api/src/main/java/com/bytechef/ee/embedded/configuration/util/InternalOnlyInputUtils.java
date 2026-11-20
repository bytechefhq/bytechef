/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class InternalOnlyInputUtils {

    private InternalOnlyInputUtils() {
    }

    public static Set<String> getInternalOnlyInputNames(Workflow workflow) {
        return WorkflowInput.of(workflow)
            .stream()
            .filter(WorkflowInput::isInternalOnly)
            .map(WorkflowInput::getName)
            .collect(Collectors.toSet());
    }

    public static Map<String, Object> removeInternalOnlyInputs(Workflow workflow, Map<String, ?> inputs) {
        Set<String> internalOnlyInputNames = getInternalOnlyInputNames(workflow);

        Map<String, Object> remainingInputs = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : inputs.entrySet()) {
            if (!internalOnlyInputNames.contains(entry.getKey())) {
                remainingInputs.put(entry.getKey(), entry.getValue());
            }
        }

        return remainingInputs;
    }

    public static Map<String, Object> retainInternalOnlyInputs(Workflow workflow, Map<String, ?> inputs) {
        Set<String> internalOnlyInputNames = getInternalOnlyInputNames(workflow);

        Map<String, Object> internalOnlyInputs = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : inputs.entrySet()) {
            if (internalOnlyInputNames.contains(entry.getKey())) {
                internalOnlyInputs.put(entry.getKey(), entry.getValue());
            }
        }

        return internalOnlyInputs;
    }

    public static @Nullable String findMissingRequiredInternalOnlyInput(
        Workflow workflow, Map<String, ?> configurationInputs) {

        for (WorkflowInput workflowInput : WorkflowInput.of(workflow)) {
            if (!workflowInput.isInternalOnly() || !workflowInput.isRequired()) {
                continue;
            }

            Object value = configurationInputs.get(workflowInput.getName());

            if (value == null || (value instanceof String string && string.isBlank())) {
                return workflowInput.getName();
            }
        }

        return null;
    }

    public static String removeInternalOnlyInputs(Workflow workflow) {
        Set<String> internalOnlyInputNames = getInternalOnlyInputNames(workflow);
        String workflowDefinition = workflow.getDefinition();

        if (internalOnlyInputNames.isEmpty()) {
            return workflowDefinition;
        }

        Map<String, Object> definition = new LinkedHashMap<>(readDefinition(workflow));

        Object inputs = definition.get(WorkflowConstants.INPUTS);

        if (!(inputs instanceof List<?> inputList)) {
            return workflowDefinition;
        }

        List<Object> remainingInputs = new ArrayList<>();

        for (Object input : inputList) {
            if (input instanceof Map<?, ?> inputMap &&
                internalOnlyInputNames.contains(inputMap.get(WorkflowConstants.NAME))) {
                continue;
            }

            remainingInputs.add(input);
        }

        definition.put(WorkflowConstants.INPUTS, remainingInputs);

        return JsonUtils.write(definition);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> readDefinition(Workflow workflow) {
        String workflowDefinition = workflow.getDefinition();

        if (workflow.getFormat() == Workflow.Format.JSON) {
            return JsonUtils.readMap(workflowDefinition);
        }

        Map<String, Object> definition;

        try {
            definition = WorkflowReader.readWorkflowMap(
                new WorkflowResource(
                    workflow.getId(), Map.of(),
                    new ByteArrayResource(workflowDefinition.getBytes(StandardCharsets.UTF_8)), workflow.getFormat()));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Workflow definition could not be read", exception);
        }

        Object tasks = definition.get(WorkflowConstants.TASKS);

        if (tasks instanceof List<?> taskList) {
            for (Object task : taskList) {
                if (task instanceof Map<?, ?> taskMap) {
                    ((Map<String, Object>) taskMap).remove(WorkflowConstants.TASK_NUMBER);
                }
            }
        }

        return definition;
    }
}
