/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface SubAgentAdvisorContributor {

    ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext);
}
