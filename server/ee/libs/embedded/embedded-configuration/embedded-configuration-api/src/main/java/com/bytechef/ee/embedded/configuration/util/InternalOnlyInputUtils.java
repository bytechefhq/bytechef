/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class InternalOnlyInputUtils {

    private static final String INPUTS = "inputs";
    private static final String INTERNAL_ONLY = "internalOnly";

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

    @SuppressWarnings("unchecked")
    public static String removeInternalOnlyInputs(String workflowDefinition) {
        Map<String, Object> definition =
            new LinkedHashMap<>((Map<String, Object>) JsonUtils.readMap(workflowDefinition));

        Object inputs = definition.get(INPUTS);

        if (!(inputs instanceof List<?> inputList)) {
            return workflowDefinition;
        }

        List<Object> remainingInputs = new ArrayList<>();

        for (Object input : inputList) {
            if (input instanceof Map<?, ?> inputMap && Boolean.TRUE.equals(inputMap.get(INTERNAL_ONLY))) {
                continue;
            }

            remainingInputs.add(input);
        }

        definition.put(INPUTS, remainingInputs);

        return JsonUtils.write(definition);
    }
}
