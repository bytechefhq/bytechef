/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * A tool the calling app may execute. {@code name} is {@code <BINDING_KEY>__<TOOL_NAME>} in upper snake case, unique
 * per app, for LLM function calling; {@code parameters} is the JSON Schema of the tool input.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppToolDTO(
    String name, String bindingKey, String componentName, String toolName, @Nullable String description,
    String parameters) {
}
