/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;

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
}
