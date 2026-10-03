/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.chat.memory.inmemory.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * The h2 profile switches chat memory to in-memory storage. Its provider value must be one the in-memory configuration
 * activates on: a value it does not match leaves the bean out, and chat memory silently falls back to Spring AI's
 * default, which keeps a much shorter window.
 *
 * @author Ivica Cardic
 */
class H2ProfileChatMemoryTest {

    @Test
    void testTheH2ProfileActivatesTheInMemoryChatMemory() throws IOException {
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(
            "application-h2", new ClassPathResource("config/application-h2.yml"));

        Object provider = propertySources.getFirst()
            .getProperty("bytechef.ai.memory.provider");

        new ApplicationContextRunner()
            .withPropertyValues("bytechef.ai.memory.provider=" + provider)
            .withBean(InMemoryChatMemoryRepository.class, InMemoryChatMemoryRepository::new)
            .withUserConfiguration(InMemoryChatMemoryConfiguration.class)
            .run(context -> assertThat(context).hasSingleBean(ChatMemory.class));
    }
}
