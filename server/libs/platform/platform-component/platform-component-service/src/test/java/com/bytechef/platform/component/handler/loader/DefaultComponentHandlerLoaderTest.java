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

package com.bytechef.platform.component.handler.loader;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.slack.SlackComponentHandler;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ProviderEntry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Covers the per-component loading API used by the build-time component index: resolving a single handler by its
 * {@code ServiceLoader} provider class name and enumerating provider class names for index generation.
 *
 * @author Ivica Cardic
 */
public class DefaultComponentHandlerLoaderTest {

    private final DefaultComponentHandlerLoader defaultComponentHandlerLoader = new DefaultComponentHandlerLoader();

    @Test
    public void testGetKind() {
        assertThat(defaultComponentHandlerLoader.getKind()).isEqualTo("default");
    }

    @Test
    public void testLoadComponentHandlerByProviderClassName() {
        Optional<ComponentHandlerEntry> componentHandlerEntryOptional =
            defaultComponentHandlerLoader.loadComponentHandler(SlackComponentHandler.class.getName());

        assertThat(componentHandlerEntryOptional).isPresent();

        ComponentHandlerEntry componentHandlerEntry = componentHandlerEntryOptional.orElseThrow();

        ComponentHandler componentHandler = componentHandlerEntry.componentHandler();

        assertThat(componentHandler.getName()).isEqualTo("slack");
        assertThat(componentHandlerEntry.componentTaskHandlerFunction()).isNotNull();
    }

    @Test
    public void testLoadComponentHandlerReturnsEmptyForUnknownProviderClassName() {
        Optional<ComponentHandlerEntry> componentHandlerEntryOptional =
            defaultComponentHandlerLoader.loadComponentHandler("com.example.DoesNotExistComponentHandler");

        assertThat(componentHandlerEntryOptional).isEmpty();
    }

    @Test
    public void testLoadProviderEntriesExposesProviderClassNames() {
        List<ProviderEntry> providerEntries = defaultComponentHandlerLoader.loadProviderEntries();

        assertThat(providerEntries)
            .extracting(ProviderEntry::providerClassName)
            .contains(SlackComponentHandler.class.getName());

        ProviderEntry slackProviderEntry = providerEntries.stream()
            .filter(providerEntry -> SlackComponentHandler.class.getName()
                .equals(providerEntry.providerClassName()))
            .findFirst()
            .orElseThrow();

        ComponentHandlerEntry componentHandlerEntry = slackProviderEntry.componentHandlerEntry();

        ComponentHandler componentHandler = componentHandlerEntry.componentHandler();

        assertThat(componentHandler.getName()).isEqualTo("slack");
    }

    @Test
    public void testLoadComponentHandlersAndProviderEntriesReturnSameComponents() {
        List<ComponentHandlerEntry> componentHandlerEntries = defaultComponentHandlerLoader.loadComponentHandlers();

        List<ProviderEntry> providerEntries = defaultComponentHandlerLoader.loadProviderEntries();

        List<String> bulkComponentNames = componentHandlerEntries.stream()
            .map(componentHandlerEntry -> {
                ComponentHandler componentHandler = componentHandlerEntry.componentHandler();

                return componentHandler.getName();
            })
            .sorted()
            .toList();

        List<String> providerComponentNames = providerEntries.stream()
            .map(providerEntry -> {
                ComponentHandlerEntry componentHandlerEntry = providerEntry.componentHandlerEntry();

                ComponentHandler componentHandler = componentHandlerEntry.componentHandler();

                return componentHandler.getName();
            })
            .sorted()
            .toList();

        assertThat(providerComponentNames).isEqualTo(bulkComponentNames);
    }
}
