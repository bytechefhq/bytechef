/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import com.bytechef.atlas.configuration.domain.Workflow;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

        Set<String> internalOnlyInputNames = InternalOnlyInputUtils.getInternalOnlyInputNames(workflow);

        Map<String, Object> inputs = new LinkedHashMap<>(configurationInputs);

        for (Map.Entry<String, ?> entry : instanceInputs.entrySet()) {
            if (!internalOnlyInputNames.contains(entry.getKey())) {
                inputs.put(entry.getKey(), entry.getValue());
            }
        }

        return inputs;
    }
}
