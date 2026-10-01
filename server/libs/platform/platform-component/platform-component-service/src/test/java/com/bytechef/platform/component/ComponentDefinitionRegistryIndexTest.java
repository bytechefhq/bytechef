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

package com.bytechef.platform.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.slack.SlackComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.index.ComponentIndex;
import com.bytechef.platform.component.index.ComponentIndexGenerator;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionRegistryIndexTest {

    @TempDir
    Path tempDir;

    private static ComponentDefinitionRegistry createRegistry(ComponentIndex componentIndex) {
        return createRegistry(componentIndex, List.of(), List::of);
    }

    private static ComponentDefinitionRegistry createRegistry(
        ComponentIndex componentIndex, List<String> excludeComponentNames,
        Supplier<List<ComponentHandlerEntry>> componentHandlerEntriesSupplier) {

        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        ApplicationProperties.Component.Registry registry = new ApplicationProperties.Component.Registry();

        registry.setExclude(excludeComponentNames);

        component.setRegistry(registry);
        applicationProperties.setComponent(component);

        return new ComponentDefinitionRegistry(
            applicationProperties, List.of(), componentHandlerEntriesSupplier, List.of(),
            () -> Optional.of(componentIndex));
    }

    private static ComponentIndex createSlackIndex() {
        SlackComponentHandler slackComponentHandler = new SlackComponentHandler();

        ComponentDefinition slackComponentDefinition = slackComponentHandler.getDefinition();

        List<ComponentIndex.ItemSummary> actionSummaries = slackComponentDefinition.getActions()
            .stream()
            .map(actionDefinition -> new ComponentIndex.ItemSummary(
                actionDefinition.getName(), actionDefinition.getTitle()
                    .orElse(null),
                actionDefinition.getDescription()
                    .orElse(null)))
            .toList();

        int version = slackComponentDefinition.getVersion();

        return new ComponentIndex(
            List.of(
                new ComponentIndex.Entry(
                    "slack", version, "Slack",
                    "Slack is a messaging platform for teams to communicate and collaborate.",
                    "path:assets/slack.svg", null, null,
                    new ComponentIndex.ConnectionSummary(1, true), actionSummaries, null, null, null, null,
                    List.of("channel"), SlackComponentHandler.class.getName(), "default")));
    }

    @Test
    public void testGetStaticComponentDefinitionsServesStubsFromIndex() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

        List<ComponentDefinition> componentDefinitions = componentDefinitionRegistry.getStaticComponentDefinitions();

        assertThat(componentDefinitions)
            .extracting(ComponentDefinition::getName)
            .contains("slack", "manual", "missing");

        ComponentDefinition slackStub = componentDefinitions.stream()
            .filter(componentDefinition -> "slack".equals(componentDefinition.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(slackStub.getTitle()).contains("Slack");
        assertThat(slackStub.getActions()).isNotEmpty();
        assertThat(slackStub.getConnection()).isPresent();

        assertThat(slackStub.getActions()
            .getFirst()
            .getProperties()).isEmpty();
    }

    @Test
    public void testGetComponentDefinitionLoadsFullComponentOnDemand() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition("slack", 1);

        assertThat(componentDefinition.getActions()
            .stream()
            .anyMatch(actionDefinition -> !actionDefinition.getProperties()
                .isEmpty()))
                    .isTrue();

        assertThat(componentDefinition.getConnection()
            .orElseThrow()
            .getAuthorizations()).isNotEmpty();
    }

    @Test
    public void testGetComponentDefinitionsForUnknownNameReturnsEmpty() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

        assertThat(componentDefinitionRegistry.getComponentDefinitions("nonexistent")).isEmpty();
    }

    @Test
    public void testGetComponentDefinitionsReturnsFullDefinitionsNotStubs() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

        List<ComponentDefinition> componentDefinitions = componentDefinitionRegistry.getComponentDefinitions();

        assertThat(componentDefinitions)
            .extracting(ComponentDefinition::getName)
            .contains("slack", "manual", "missing");

        ComponentDefinition slackComponentDefinition = componentDefinitions.stream()
            .filter(componentDefinition -> "slack".equals(componentDefinition.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(slackComponentDefinition.getActions()
            .stream()
            .anyMatch(actionDefinition -> !actionDefinition.getProperties()
                .isEmpty()))
                    .isTrue();
    }

    @Test
    public void testFetchComponentDefinitionByVersionFromIndex() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

        assertThat(componentDefinitionRegistry.fetchComponentDefinition("slack", 1)).isPresent();
        assertThat(componentDefinitionRegistry.fetchComponentDefinition("slack", 99)).isEmpty();
        assertThat(componentDefinitionRegistry.fetchComponentDefinition("slack", null)
            .orElseThrow()
            .getVersion()).isEqualTo(1);
    }

    @Test
    public void testHasComponentDefinitionFromIndex() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

        assertThat(componentDefinitionRegistry.hasComponentDefinition("slack", 1)).isTrue();
        assertThat(componentDefinitionRegistry.hasComponentDefinition("slack", null)).isTrue();
        assertThat(componentDefinitionRegistry.hasComponentDefinition("nonexistent", null)).isFalse();
    }

    @Test
    public void testExcludedComponentIsHiddenFromIndexPaths() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(
            createSlackIndex(), List.of("slack"), List::of);

        assertThat(componentDefinitionRegistry.getStaticComponentDefinitions())
            .extracting(ComponentDefinition::getName)
            .doesNotContain("slack");

        assertThat(componentDefinitionRegistry.getComponentDefinitions("slack")).isEmpty();
    }

    @Test
    public void testIndexEntryWithMissingProviderClassIsSkipped() {
        ComponentIndex componentIndex = new ComponentIndex(
            List.of(
                new ComponentIndex.Entry(
                    "ghost", 1, "Ghost", null, null, null, null, null, null, null, null, null, null, null,
                    "com.bytechef.component.ghost.DoesNotExistComponentHandler", "default")));

        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(componentIndex);

        assertThat(componentDefinitionRegistry.getStaticComponentDefinitions())
            .extracting(ComponentDefinition::getName)
            .contains("ghost");

        assertThat(componentDefinitionRegistry.getComponentDefinitions("ghost")).isEmpty();
    }

    @Test
    public void testIndexPathsNeverInvokeBulkEntriesSupplier() {
        AtomicInteger bulkLoadCount = new AtomicInteger();

        Supplier<List<ComponentHandlerEntry>> countingSupplier = () -> {
            bulkLoadCount.incrementAndGet();

            return List.of();
        };

        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(
            createSlackIndex(), List.of(), countingSupplier);

        componentDefinitionRegistry.getStaticComponentDefinitions();
        componentDefinitionRegistry.getComponentDefinition("slack", 1);
        componentDefinitionRegistry.getComponentDefinitions();

        assertThat(bulkLoadCount.get()).isZero();
    }

    @Test
    public void testGetActionDefinitionThroughIndexPath() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

        ComponentDefinition slackComponentDefinition = componentDefinitionRegistry.getComponentDefinition("slack", 1);

        String actionName = slackComponentDefinition.getActions()
            .getFirst()
            .getName();

        assertThat(componentDefinitionRegistry.getActionDefinition("slack", 1, actionName)
            .getName()).isEqualTo(actionName);
    }

    @Test
    public void testStubCarriesListViewMetadataFromSyntheticEntry() {
        ComponentIndex componentIndex = new ComponentIndex(
            List.of(
                new ComponentIndex.Entry(
                    "fake", 3, "Fake", "A fake component.", "path:assets/fake.svg",
                    List.of(new ComponentIndex.CategorySummary("communication", "Communication")),
                    List.of("tag1"),
                    new ComponentIndex.ConnectionSummary(2, false),
                    List.of(new ComponentIndex.ItemSummary("doIt", "Do It", "Does it.")),
                    List.of(new ComponentIndex.TriggerSummary("onEvent", "On Event", null, "STATIC_WEBHOOK")),
                    List.of(
                        new ComponentIndex.ClusterElementSummary(
                            "myTool", "My Tool", null, "TOOLS", "tools", "Tools", true, false)),
                    null, null, List.of("channel"), "does.not.Matter", "default")));

        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(componentIndex);

        ComponentDefinition stub = componentDefinitionRegistry.getStaticComponentDefinitions()
            .stream()
            .filter(componentDefinition -> "fake".equals(componentDefinition.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(stub.getVersion()).isEqualTo(3);
        assertThat(stub.getTitle()).contains("Fake");
        assertThat(stub.getIcon()).contains("path:assets/fake.svg");
        assertThat(stub.getComponentCategories()
            .getFirst()
            .name()).isEqualTo("communication");
        assertThat(stub.getConnection()
            .orElseThrow()
            .getVersion()).isEqualTo(2);
        assertThat(stub.getConnection()
            .orElseThrow()
            .getAuthorizationRequired()).isFalse();
        assertThat(stub.getActions()).hasSize(1);
        assertThat(stub.getTriggers()
            .getFirst()
            .getType()).isEqualTo(com.bytechef.component.definition.TriggerDefinition.TriggerType.STATIC_WEBHOOK);
        assertThat(stub.getClusterElements()
            .getFirst()
            .getType()
            .key()).isEqualTo("tools");
        assertThat(stub.getInputs()).hasSize(1);
    }

    @Test
    public void testStubOfClusterRootEntryReportsItsClusterElementTypes() {
        ComponentIndex componentIndex = new ComponentIndex(
            List.of(
                new ComponentIndex.Entry(
                    "fakeRoot", 1, "Fake Root", null, null, null, null, null, null, null, null,
                    List.of(new ComponentIndex.ClusterElementTypeSummary("TOOLS", "tools", "Tools", true, false)),
                    null, null, "does.not.Matter", "default")));

        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(componentIndex);

        ComponentDefinition stub = componentDefinitionRegistry.getStaticComponentDefinitions()
            .stream()
            .filter(componentDefinition -> "fakeRoot".equals(componentDefinition.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(stub).isInstanceOf(ClusterRootComponentDefinition.class);

        assertThat(((ClusterRootComponentDefinition) stub).getClusterElementTypes())
            .extracting(ClusterElementType::key)
            .containsExactly("tools");

        assertThat(stub.getTitle()).contains("Fake Root");
        assertThat(stub.getVersion()).isEqualTo(1);
    }

    @Test
    public void testGetStaticComponentDefinitionsFallsBackToFullLoadingOnBrokenStub() {
        ComponentIndex brokenComponentIndex = new ComponentIndex(
            List.of(
                new ComponentIndex.Entry(
                    "slack", 1, "Slack", null, null, null, null, null, null,
                    List.of(new ComponentIndex.TriggerSummary("newMessage", null, null, "NOT_A_REAL_TRIGGER_TYPE")),
                    null, null, null, null, SlackComponentHandler.class.getName(), "default")));

        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(brokenComponentIndex);

        List<ComponentDefinition> componentDefinitions = componentDefinitionRegistry.getStaticComponentDefinitions();

        ComponentDefinition slackComponentDefinition = componentDefinitions.stream()
            .filter(componentDefinition -> "slack".equals(componentDefinition.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(slackComponentDefinition.getActions())
            .anySatisfy(actionDefinition -> assertThat(actionDefinition.getProperties()).isNotEmpty());
    }

    @Test
    public void testGeneratorRoundTrip() throws Exception {
        Path indexPath = tempDir.resolve("META-INF/bytechef/component-index.json");

        ComponentIndexGenerator.main(new String[] {
            indexPath.toString()
        });

        try (URLClassLoader classLoader = new URLClassLoader(
            new URL[] {
                tempDir.toUri()
                    .toURL()
            }, null)) {

            Optional<ComponentIndex> componentIndexOptional = ComponentIndex.load(classLoader);

            assertThat(componentIndexOptional).isPresent();

            ComponentIndex componentIndex = componentIndexOptional.orElseThrow();

            ComponentIndex.Entry slackEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "slack".equals(entry.name()))
                .findFirst()
                .orElseThrow();

            assertThat(slackEntry.providerClassName()).isEqualTo(SlackComponentHandler.class.getName());
            assertThat(slackEntry.loaderKind()).isEqualTo("default");
            assertThat(slackEntry.actions()).isNotEmpty();
            assertThat(slackEntry.connection()).isNotNull();
        }

        assertThat(Files.readString(indexPath)).contains("\"slack\"");
    }
}
