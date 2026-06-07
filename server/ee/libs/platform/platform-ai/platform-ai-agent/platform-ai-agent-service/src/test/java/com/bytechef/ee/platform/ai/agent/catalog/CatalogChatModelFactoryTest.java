/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CatalogChatModelFactoryTest {

    private final CatalogChatModelFactory factory = new CatalogChatModelFactory();

    @Test
    void testCreateChatModelForOpenAiReturnsModel() {
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.OPEN_AI, "gpt-4o", "sk-test-key");

        assertThat(chatModel).isNotNull();
    }

    @Test
    void testCreateChatModelForCatalogOnlyConnectionProviderReturnsNull() {
        // Azure needs a deployment endpoint the catalog doesn't store; v1 returns null -> caller falls back.
        org.springframework.ai.chat.model.ChatModel chatModel =
            factory.createChatModel(Provider.AZURE_OPEN_AI, "gpt-4o", "sk-test-key");

        assertThat(chatModel).isNull();
    }
}
