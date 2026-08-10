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
 * Contributes advisors (and, where needed, advisor params) to a subagent delegate's request, given the
 * {@code ToolContext} the delegate forwarded from the parent agent.
 *
 * <p>
 * Returns the spec rather than a list of advisors because a contributor may need to set request params — the session
 * memory contributor must publish the conversation key the memory advisor resolves its session from. Implementations
 * MUST return the spec produced by their own calls rather than the argument: the Spring AI implementation happens to
 * mutate and return {@code this}, but the contract does not guarantee it.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface SubAgentAdvisorContributor {

    ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext);
}
