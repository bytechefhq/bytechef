/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.state.State;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Per-request resolver of an override {@link ChatClient} for Copilot agents. Used to swap the LLM at runtime when the
 * client has supplied a user-selected (provider, model) pair via AG-UI state keys, instead of the workspace-default
 * {@code @Primary ChatModel} the agent was built against.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface OverrideChatClientResolver {

    @Nullable
    ChatClient resolve(State state);
}
