/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Joined view: one {@link AiHubTaskTool} row plus its parent {@link AiHubTaskComponent}'s context. The agent
 * dynamic-callback resolver consumes this directly — every field needed to build a {@code ClusterElementToolCallback}
 * for the LLM is here, no additional DB round-trips.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AiHubTaskToolBinding(
    long taskToolId, long taskComponentId, long taskId, String componentName,
    int componentVersion, String clusterElementName, @Nullable Long connectionId, int environment,
    Map<String, ?> parameters) {
}
