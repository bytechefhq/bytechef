/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolUsageRecorder {

    void recordTool(
        ToolUsageContext context, String toolName, int unitCount, long durationMs,
        @Nullable Map<String, Object> metadata);
}
