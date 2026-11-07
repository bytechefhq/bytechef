/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

import java.util.HashMap;
import java.util.Map;

/**
 * @version ee
 * @author Ivica Cardic
 */
public final class ComponentRuleConditionStubContext {

    private ComponentRuleConditionStubContext() {
    }

    public static Map<String, Object> get() {
        Map<String, Object> context = new HashMap<>();

        context.put("inputParameters", Map.of());
        context.put("componentName", "");
        context.put("toolName", "");
        context.put("toolCallName", "");
        context.put("connectionId", null);
        context.put("output", Map.of());

        return context;
    }
}
