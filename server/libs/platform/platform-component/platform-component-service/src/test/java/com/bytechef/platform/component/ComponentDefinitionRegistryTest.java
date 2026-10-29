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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.slack.SlackComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoaderKind;
import com.bytechef.platform.component.index.ComponentIndex;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class ComponentDefinitionRegistryTest {

    private static final ClusterElementType REQUIRED_MODEL_TYPE = new ClusterElementType(
        "MODEL", "model", "Model", true);

    private static ComponentDefinitionRegistry createRegistry() {
        return createRegistry(new SlackComponentHandler());
    }

    private static ComponentDefinitionRegistry createRegistry(ComponentHandler componentHandler) {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());
        applicationProperties.setComponent(component);

        return new ComponentDefinitionRegistry(
            applicationProperties,
            List.of(componentHandler),
            List::of,
            List.of());
    }

    private static ComponentDefinitionRegistry createClusterElementRegistry() {
        return createRegistry(
            () -> ComponentDsl.component("typedLookup")
                .title("Typed Lookup")
                .clusterElements(
                    ComponentDsl.<Object>clusterElement("chatModel")
                        .title("Chat Model")
                        .type(REQUIRED_MODEL_TYPE)
                        .object(Object::new)));
    }

    @Test
    public void testGetClusterElementDefinitionMatchesTheTypeByName() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createClusterElementRegistry();

        ClusterElementDefinition<?> clusterElementDefinition = componentDefinitionRegistry.getClusterElementDefinition(
            "typedLookup", 1, "chatModel", new ClusterElementType("MODEL", "model", "Model", false));

        assertThat(clusterElementDefinition.getName())
            .isEqualTo("chatModel");
        assertThat(clusterElementDefinition.getType())
            .isEqualTo(REQUIRED_MODEL_TYPE);
    }

    @Test
    public void testGetClusterElementDefinitionExcludesATypeWithADifferentName() {
        ComponentDefinitionRegistry componentDefinitionRegistry = createClusterElementRegistry();

        ClusterElementType chatMemoryType = new ClusterElementType("CHAT_MEMORY", "model", "Model", true);

        assertThatThrownBy(
            () -> componentDefinitionRegistry.getClusterElementDefinition(
                "typedLookup", 1, "chatModel", chatMemoryType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                        "The component 'typedLookup' does not contain the 'chatModel' cluster element with type " +
                            "'CHAT_MEMORY'.");
    }

    @Disabled
    @Test
    public void testGetActionDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetActionDefinitions() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetActionProperty() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetAuthorization() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentConnectionDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentDefinitions() {
        // TODO
    }

    @Test
    public void testGetComponentInputProperty() throws Exception {
        ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry();

        Property property = componentDefinitionRegistry.getComponentInputProperty(
            "slack", 1, "channel", "channel", ParametersFactory.create(Map.of()),
            ParametersFactory.create((ComponentConnection) null), Map.of(), Mockito.mock(ActionContext.class));

        assertThat(property.getName())
            .isEqualTo("channel");
        assertThat(property)
            .isInstanceOf(DynamicOptionsProperty.class);
    }

    @Disabled
    @Test
    public void testGetConnectionDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetConnectionDefinitions() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTriggerDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTriggerDefinitions() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTriggerProperty() {
        // TODO
    }

    @Nested
    @ExtendWith(ObjectMapperSetupExtension.class)
    class IndexTest {

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
                .orElse(List.of())
                .stream()
                .map(actionDefinition -> new ComponentIndex.ItemSummary(
                    actionDefinition.getName(), actionDefinition.getTitle()
                        .orElse(null),
                    actionDefinition.getDescription()
                        .orElse(null)))
                .toList();

            return new ComponentIndex(
                List.of(
                    new ComponentIndex.Entry(
                        "slack", slackComponentDefinition.getVersion(), "Slack",
                        "Slack is a messaging platform for teams to communicate and collaborate.",
                        "path:assets/slack.svg", null, null,
                        new ComponentIndex.ConnectionSummary(1, true), actionSummaries, null, null,
                        List.of("channel"), null, SlackComponentHandler.class.getName(),
                        ComponentHandlerLoaderKind.DEFAULT)));
        }

        @Test
        public void testGetStaticComponentDefinitionsServesStubsFromIndex() {
            ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

            List<ComponentDefinition> componentDefinitions =
                componentDefinitionRegistry.getStaticComponentDefinitions();

            assertThat(componentDefinitions)
                .extracting(ComponentDefinition::getName)
                .contains("slack", "manual", "missing");

            ComponentDefinition slackStub = componentDefinitions.stream()
                .filter(componentDefinition -> "slack".equals(componentDefinition.getName()))
                .findFirst()
                .orElseThrow();

            assertThat(slackStub.getTitle()).contains("Slack");
            assertThat(slackStub.getActions()
                .orElse(List.of())).isNotEmpty();
            assertThat(slackStub.getConnection()).isPresent();

            assertThat(slackStub.getActions()
                .orElseThrow()
                .getFirst()
                .getProperties()
                .orElse(List.of())).isEmpty();
        }

        @Test
        public void testGetComponentDefinitionLoadsFullComponentOnDemand() {
            ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

            ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition("slack", 1);

            assertThat(componentDefinition.getActions()
                .orElseThrow()
                .stream()
                .anyMatch(actionDefinition -> !actionDefinition.getProperties()
                    .orElse(List.of())
                    .isEmpty()))
                        .isTrue();

            assertThat(componentDefinition.getConnection()
                .orElseThrow()
                .getAuthorizations()
                .orElse(List.of())).isNotEmpty();
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
                .orElseThrow()
                .stream()
                .anyMatch(actionDefinition -> !actionDefinition.getProperties()
                    .orElse(List.of())
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
                        "ghost", 1, "Ghost", null, null, null, null, null, null, null, null, null, null,
                        "com.bytechef.component.ghost.DoesNotExistComponentHandler",
                        ComponentHandlerLoaderKind.DEFAULT)));

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

            ComponentDefinition slackComponentDefinition =
                componentDefinitionRegistry.getComponentDefinition("slack", 1);

            String actionName = slackComponentDefinition.getActions()
                .orElseThrow()
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
                        List.of(
                            new ComponentIndex.TriggerSummary("onEvent", "On Event", null, TriggerType.STATIC_WEBHOOK)),
                        List.of(
                            new ComponentIndex.ClusterElementSummary(
                                "myTool", "My Tool", null, "TOOLS", "tools", "Tools", true, false)),
                        List.of("channel"), UnifiedApiCategory.CRM, "does.not.Matter",
                        ComponentHandlerLoaderKind.DEFAULT)));

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
                .orElseThrow()
                .getFirst()
                .name()).isEqualTo("communication");
            assertThat(stub.getConnection()
                .orElseThrow()
                .getVersion()).isEqualTo(2);
            assertThat(stub.getConnection()
                .orElseThrow()
                .getAuthorizationRequired()).contains(false);
            assertThat(stub.getActions()
                .orElseThrow()).hasSize(1);
            assertThat(stub.getTriggers()
                .orElseThrow()
                .getFirst()
                .getType()).isEqualTo(TriggerType.STATIC_WEBHOOK);
            assertThat(stub.getClusterElements()
                .orElseThrow()
                .getFirst()
                .getType()
                .key()).isEqualTo("tools");
            assertThat(stub.getInputs()
                .orElseThrow()).hasSize(1);
            assertThat(stub.getUnifiedApi()
                .map(UnifiedApiDefinition::getCategory)).contains(UnifiedApiCategory.CRM);

            com.bytechef.platform.component.domain.ComponentDefinition domainComponentDefinition =
                new com.bytechef.platform.component.domain.ComponentDefinition(stub);

            assertThat(domainComponentDefinition.getUnifiedApiCategory()).isEqualTo(UnifiedApiCategory.CRM);
        }

        @Test
        public void testStubWithoutUnifiedApiCategoryHasNoUnifiedApi() {
            ComponentDefinitionRegistry componentDefinitionRegistry = createRegistry(createSlackIndex());

            ComponentDefinition slackStub = componentDefinitionRegistry.getStaticComponentDefinitions()
                .stream()
                .filter(componentDefinition -> "slack".equals(componentDefinition.getName()))
                .findFirst()
                .orElseThrow();

            assertThat(slackStub.getUnifiedApi()).isEmpty();

            com.bytechef.platform.component.domain.ComponentDefinition domainComponentDefinition =
                new com.bytechef.platform.component.domain.ComponentDefinition(slackStub);

            assertThat(domainComponentDefinition.getUnifiedApiCategory()).isNull();
        }

        @Test
        public void testCorruptTriggerTypeInIndexFallsBackToFullLoading() throws Exception {
            Path indexPath = tempDir.resolve(ComponentIndex.RESOURCE_PATH);

            Files.createDirectories(Objects.requireNonNull(indexPath.getParent()));
            Files.writeString(indexPath, """
                {"entries":[
                  {"name":"slack","version":1,"providerClassName":"com.bytechef.component.slack.SlackComponentHandler",
                   "loaderKind":"default","triggers":[{"name":"newMessage","type":"NOT_A_REAL_TRIGGER_TYPE"}]}
                ]}""");

            AtomicInteger bulkLoadCount = new AtomicInteger();

            Supplier<List<ComponentHandlerEntry>> countingSupplier = () -> {
                bulkLoadCount.incrementAndGet();

                return List.of();
            };

            try (URLClassLoader classLoader = new URLClassLoader(
                new URL[] {
                    tempDir.toUri()
                        .toURL()
                }, null)) {

                ApplicationProperties applicationProperties = new ApplicationProperties();
                ApplicationProperties.Component component = new ApplicationProperties.Component();

                component.setRegistry(new ApplicationProperties.Component.Registry());
                applicationProperties.setComponent(component);

                ComponentDefinitionRegistry componentDefinitionRegistry = new ComponentDefinitionRegistry(
                    applicationProperties, List.of(new SlackComponentHandler()), countingSupplier, List.of(),
                    () -> ComponentIndex.load(classLoader));

                List<ComponentDefinition> componentDefinitions =
                    componentDefinitionRegistry.getStaticComponentDefinitions();

                assertThat(componentDefinitions)
                    .filteredOn(componentDefinition -> "slack".equals(componentDefinition.getName()))
                    .singleElement()
                    .satisfies(slackComponentDefinition -> assertThat(slackComponentDefinition.getActions()
                        .orElse(List.of()))
                            .anySatisfy(actionDefinition -> assertThat(actionDefinition.getProperties()
                                .orElse(List.of())).isNotEmpty()));
            }

            assertThat(bulkLoadCount.get()).isOne();
        }
    }
}
