/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AiHubChatToolBinding(
    long chatToolId, long chatComponentId, long chatId, String componentName,
    int componentVersion, String clusterElementName, @Nullable Long connectionId, int environment,
    Map<String, ?> parameters, boolean requiresApproval) {
}
