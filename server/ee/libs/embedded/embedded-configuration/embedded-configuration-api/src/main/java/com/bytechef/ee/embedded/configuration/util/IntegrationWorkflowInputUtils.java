/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class IntegrationWorkflowInputUtils {

    private IntegrationWorkflowInputUtils() {
    }

    public static Map<String, Object> mergeInputs(
        Workflow workflow, Map<String, ?> configurationInputs, Map<String, ?> instanceInputs) {

        Set<String> internalOnlyInputNames = getInternalOnlyInputNames(workflow);

        Map<String, Object> overridingInstanceInputs = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : instanceInputs.entrySet()) {
            String inputName = entry.getKey();

            if (internalOnlyInputNames.contains(inputName) && configurationInputs.get(inputName) != null) {
                continue;
            }

            overridingInstanceInputs.put(inputName, entry.getValue());
        }

        return MapUtils.concat(new LinkedHashMap<String, Object>(configurationInputs), overridingInstanceInputs);
    }

    public static Map<String, Object> preserveInternalOnlyInputs(
        Workflow workflow, Map<String, ?> requestedInputs, Map<String, ?> storedInputs) {

        Set<String> internalOnlyInputNames = getInternalOnlyInputNames(workflow);

        Map<String, Object> inputs = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : requestedInputs.entrySet()) {
            if (!internalOnlyInputNames.contains(entry.getKey())) {
                inputs.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, ?> entry : storedInputs.entrySet()) {
            if (internalOnlyInputNames.contains(entry.getKey())) {
                inputs.put(entry.getKey(), entry.getValue());
            }
        }

        return inputs;
    }

    private static Set<String> getInternalOnlyInputNames(Workflow workflow) {
        return WorkflowInput.of(workflow)
            .stream()
            .filter(WorkflowInput::isInternalOnly)
            .map(WorkflowInput::getName)
            .collect(Collectors.toSet());
    }
}
