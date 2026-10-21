/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A workflow bound to an app. When the binding no longer resolves to a callable workflow, {@code available} is false,
 * {@code name} is the workflow key and both schemas are null.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppWorkflowDTO(
    String workflowKey, String name, @Nullable String description, @Nullable Map<String, Object> inputSchema,
    @Nullable Map<String, Object> outputSchema, boolean available) {
}
