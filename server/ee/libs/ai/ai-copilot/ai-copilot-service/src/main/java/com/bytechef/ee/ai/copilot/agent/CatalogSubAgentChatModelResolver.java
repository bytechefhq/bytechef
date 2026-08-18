/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link ChatModel} a caller picked (AI Hub composer or Copilot panel toolbar) for a delegate subagent
 * tool call, from the {@link AgentToolInvocationContext} keys the delegate forwards. Deliberately not gated on
 * {@code bytechef.ai.copilot.enabled} — the AI Hub needs this resolver too, and its own gate is independent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogSubAgentChatModelResolver implements SubAgentChatModelResolver {

    private static final Logger log = LoggerFactory.getLogger(CatalogSubAgentChatModelResolver.class);

    private final CatalogChatClientResolver catalogChatClientResolver;

    @SuppressFBWarnings("EI")
    public CatalogSubAgentChatModelResolver(CatalogChatClientResolver catalogChatClientResolver) {
        this.catalogChatClientResolver = catalogChatClientResolver;
    }

    @Override
    public @Nullable ChatModel resolve(Map<String, Object> toolContext) {
        String providerKey =
            StringUtils.asString(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY));
        String model = StringUtils.asString(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY));
        Long environmentId =
            NumberUtils.asLong(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY));

        if (providerKey == null || model == null || environmentId == null) {
            return null;
        }

        try {
            ChatModel chatModel =
                catalogChatClientResolver.resolveChatModel(providerKey, model, environmentId.intValue());

            if (chatModel == null) {
                log.warn(
                    "Subagent user-selected LLM (provider={}, model={}) could not be resolved in environment {}; "
                        + "the delegate will use its default model",
                    providerKey, model, environmentId);
            }

            return chatModel;
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Subagent user-selected LLM (provider={}, model={}) failed to resolve in environment {}; "
                    + "the delegate will use its default model",
                providerKey, model, environmentId, runtimeException);

            return null;
        }
    }
}
