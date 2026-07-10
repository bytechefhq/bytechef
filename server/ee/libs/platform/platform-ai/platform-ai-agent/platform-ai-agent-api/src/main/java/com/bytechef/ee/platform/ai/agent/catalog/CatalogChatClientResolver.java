/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Resolves an override {@link ChatClient} from the platform AI provider catalog: given a catalog provider key (e.g.
 * {@code "ai.provider.openAi"}) + model name, reads the environment-scoped platform API key and builds a Spring-AI
 * {@link ChatClient}. Returns {@code null} (caller falls back) when the key is unknown, the provider is disabled, no
 * API key is stored, or the model can't be built.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CatalogChatClientResolver {

    /**
     * Resolves a {@link ChatClient} for the given environment ordinal, catalog provider key and model name.
     *
     * @param providerKey the catalog provider key (e.g. {@code "ai.provider.openAi"})
     * @param model       the model name
     * @param environment the environment ordinal (must be within the {@code Environment} enum range; out-of-range
     *                    values fail closed and return {@code null})
     * @return a configured {@link ChatClient}, or {@code null} when the selection can't be resolved
     */
    @Nullable
    ChatClient resolve(String providerKey, String model, int environment);

    /**
     * Resolves a {@link ChatClient} bound to the environment's default (first enabled) chat provider, resolved eagerly
     * for that environment so the model call does not depend on the thread-local {@code EnvironmentContext}.
     *
     * @param environment the environment ordinal
     * @return a configured {@link ChatClient}, or {@code null} when no chat provider is enabled for the environment
     */
    @Nullable
    ChatClient resolveDefault(int environment);

    /**
     * Resolves a {@link ChatClient} for a specific preferred provider, using that provider's configured default chat
     * model. Returns {@code null} (caller falls back to {@link #resolveDefault(int)}) when the provider key is blank,
     * or the provider is disabled, unconfigured, or has no default model in the environment.
     *
     * @param providerKey the catalog provider key to prefer (e.g. {@code "anthropic"})
     * @param environment the environment ordinal
     * @return a configured {@link ChatClient}, or {@code null} when the preferred provider can't be resolved
     */
    @Nullable
    ChatClient resolvePreferred(String providerKey, int environment);

    /**
     * Resolves the environment-default {@link ChatModel} (same provider/model selection as
     * {@link #resolveDefault(int)}, optionally preferring a specific provider first) without wrapping it in a
     * {@link ChatClient}, so callers can attach their own system prompt and tools.
     *
     * @param preferredProviderKey the catalog provider key to prefer, or {@code null}/blank to use the environment
     *                             default
     * @param environment          the environment ordinal
     * @return the resolved {@link ChatModel}, or {@code null} when nothing is resolvable
     */
    @Nullable
    ChatModel resolveDefaultChatModel(@Nullable String preferredProviderKey, int environment);
}
