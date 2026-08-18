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

package com.bytechef.platform.component.index;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.agentReply;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAgentChannelDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAgentReplyDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiablePropertyGroup;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.PropertyGroup;
import com.bytechef.component.definition.Resources;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Build-time generated index of every {@code ServiceLoader}-discoverable component: the list-view metadata (name,
 * version, title, description, icon, categories, action/trigger/cluster-element summaries, connection presence) plus
 * the {@code ServiceLoader} provider class name and loader kind needed to load the full component on demand.
 *
 * <p>
 * The index is written by {@code ComponentIndexGenerator} during the application build (see the
 * {@code generateComponentIndex} Gradle task) to {@value #RESOURCE_PATH} and read back here at runtime. When no index
 * resource is present on the classpath — component module tests, apps whose build does not run the generator — the
 * registry transparently falls back to loading every component handler, so the index is purely an optimization.
 * </p>
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ComponentIndex(List<Entry> entries) {

    public static final String RESOURCE_PATH = "META-INF/bytechef/component-index.json";

    private static final Logger log = LoggerFactory.getLogger(ComponentIndex.class);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    /**
     * Loads and merges every {@value #RESOURCE_PATH} resource on the classpath. Returns empty when none exist or when
     * parsing fails, in which case callers fall back to full component loading.
     */
    public static Optional<ComponentIndex> load(ClassLoader classLoader) {
        List<Entry> entries = new ArrayList<>();

        Set<String> seenComponents = new HashSet<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(RESOURCE_PATH);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                try (InputStream inputStream = url.openStream()) {
                    ComponentIndex componentIndex = JSON_MAPPER.readValue(inputStream, ComponentIndex.class);

                    for (Entry entry : componentIndex.entries()) {
                        if (seenComponents.add(entry.name() + "/" + entry.version())) {
                            entries.add(entry);
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            log.warn(
                "Failed to read component index from classpath; falling back to full component loading", exception);

            return Optional.empty();
        }

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ComponentIndex(entries));
    }

    /**
     * Builds a lightweight {@link ComponentDefinition} carrying exactly the list-view metadata: identity, texts, icon
     * path, categories, tags, connection presence (with the effective authorization-required flag), and action /
     * trigger / cluster-element / input summaries — but no property trees and no executable functions. Stubs are served
     * ONLY for the components-list view; every detail or execution path loads the real component.
     */
    public static ComponentDefinition toStubComponentDefinition(Entry entry) {
        ModifiableComponentDefinition componentDefinition = component(entry.name())
            .version(entry.version());

        if (entry.title() != null) {
            componentDefinition.title(entry.title());
        }

        if (entry.description() != null) {
            componentDefinition.description(entry.description());
        }

        if (entry.icon() != null) {
            componentDefinition.icon(entry.icon());
        }

        List<CategorySummary> componentCategories = entry.componentCategories();

        if (componentCategories != null && !componentCategories.isEmpty()) {
            componentDefinition.categories(
                componentCategories
                    .stream()
                    .map(category -> new ComponentCategory(category.name(), category.label()))
                    .toList());
        }

        List<String> tags = entry.tags();

        if (tags != null && !tags.isEmpty()) {
            componentDefinition.tags(tags.toArray(String[]::new));
        }

        ConnectionSummary connectionSummary = entry.connection();

        if (connectionSummary != null) {
            componentDefinition.connection(
                connection()
                    .version(connectionSummary.version())
                    .authorizationRequired(connectionSummary.authorizationRequired()));
        }

        List<ItemSummary> actions = entry.actions();

        if (actions != null && !actions.isEmpty()) {
            componentDefinition.actions(
                actions
                    .stream()
                    .map(ComponentIndex::toStubActionDefinition)
                    .toList());
        }

        List<TriggerSummary> triggers = entry.triggers();

        if (triggers != null && !triggers.isEmpty()) {
            componentDefinition.triggers(
                triggers
                    .stream()
                    .map(ComponentIndex::toStubTriggerDefinition)
                    .toList());
        }

        List<ClusterElementSummary> clusterElements = entry.clusterElements();

        if (clusterElements != null && !clusterElements.isEmpty()) {
            componentDefinition.clusterElements(
                clusterElements
                    .stream()
                    .map(ComponentIndex::toStubClusterElementDefinition)
                    .toArray(ClusterElementDefinition<?>[]::new));
        }

        List<String> inputs = entry.inputs();

        if (inputs != null && !inputs.isEmpty()) {
            componentDefinition.inputs(
                inputs
                    .stream()
                    .map(ComponentDsl::propertyGroup)
                    .toArray(ModifiablePropertyGroup[]::new));
        }

        // Triggers and actions must already be attached above: agent-channel rebuilding pairs a channel against
        // them by name, and (for reply mappings) declares stub properties on them before pairing, since Task 1's
        // agentChannel(...) validates the pair eagerly at construction time.
        List<AgentChannelSummary> agentChannels = entry.agentChannels();

        if (agentChannels != null && !agentChannels.isEmpty()) {
            componentDefinition.agentChannels(
                agentChannels
                    .stream()
                    .map(agentChannelSummary -> toStubAgentChannelDefinition(componentDefinition, agentChannelSummary))
                    .toList());
        }

        List<ClusterElementTypeSummary> clusterElementTypes = entry.clusterElementTypes();

        if (clusterElementTypes == null || clusterElementTypes.isEmpty()) {
            return componentDefinition;
        }

        // ModifiableComponentDefinition is an SDK class and cannot implement the platform-side
        // ClusterRootComponentDefinition, so a cluster root's stub has to be a delegating wrapper. Without it the
        // domain wrapper finds no cluster element types and reports clusterRoot = false for every listed component,
        // since that flag is derived from the types rather than stored.
        return new StubClusterRootComponentDefinition(
            componentDefinition,
            clusterElementTypes
                .stream()
                .map(
                    clusterElementType -> new ClusterElementType(
                        clusterElementType.name(), clusterElementType.key(), clusterElementType.label(),
                        clusterElementType.multipleElements(), clusterElementType.required()))
                .toList());
    }

    private static ModifiableActionDefinition toStubActionDefinition(ItemSummary itemSummary) {
        ModifiableActionDefinition actionDefinition = action(itemSummary.name());

        if (itemSummary.title() != null) {
            actionDefinition.title(itemSummary.title());
        }

        if (itemSummary.description() != null) {
            actionDefinition.description(itemSummary.description());
        }

        return actionDefinition;
    }

    private static ModifiableTriggerDefinition toStubTriggerDefinition(TriggerSummary triggerSummary) {
        ModifiableTriggerDefinition triggerDefinition = trigger(triggerSummary.name());

        if (triggerSummary.type() != null) {
            triggerDefinition.type(TriggerType.valueOf(triggerSummary.type()));
        }

        if (triggerSummary.title() != null) {
            triggerDefinition.title(triggerSummary.title());
        }

        if (triggerSummary.description() != null) {
            triggerDefinition.description(triggerSummary.description());
        }

        return triggerDefinition;
    }

    private static ModifiableClusterElementDefinition<?> toStubClusterElementDefinition(
        ClusterElementSummary clusterElementSummary) {

        ModifiableClusterElementDefinition<Object> clusterElementDefinition = clusterElement(
            clusterElementSummary.name());

        clusterElementDefinition.type(
            new ClusterElementType(
                clusterElementSummary.typeName(), clusterElementSummary.typeKey(), clusterElementSummary.typeLabel(),
                clusterElementSummary.typeMultipleElements(), clusterElementSummary.typeRequired()));

        if (clusterElementSummary.title() != null) {
            clusterElementDefinition.title(clusterElementSummary.title());
        }

        if (clusterElementSummary.description() != null) {
            clusterElementDefinition.description(clusterElementSummary.description());
        }

        return clusterElementDefinition;
    }

    /**
     * Rebuilds an {@link AgentChannelDefinition} from its {@link AgentChannelSummary}. The summary carries the full
     * request/reply binding (paths and mapped property names), not just the channel's name, because a stub missing the
     * binding would let the workflow generator wire the wrong expressions — worse than the channel simply not appearing
     * on the stub.
     */
    private static AgentChannelDefinition toStubAgentChannelDefinition(
        ModifiableComponentDefinition componentDefinition, AgentChannelSummary agentChannelSummary) {

        TriggerDefinition triggerDefinition = componentDefinition.getTriggers()
            .stream()
            .filter(candidate -> Objects.equals(candidate.getName(), agentChannelSummary.triggerName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Index entry '%s' agent channel '%s' references unknown trigger '%s'".formatted(
                    componentDefinition.getName(), agentChannelSummary.name(), agentChannelSummary.triggerName())));

        // Rebuild the request descriptor from the summary. Stub triggers carry no output schema, so the path check
        // against the output is skipped for them anyway - the descriptor is what must survive, since the generator
        // reads its paths and a stub missing them would wire the workflow to the wrong expressions. For the same
        // reason the index records no unverifiedPaths reason: it exists only to relax that skipped check, so a stub
        // that dropped it validates identically.
        if (triggerDefinition instanceof ModifiableTriggerDefinition modifiableTrigger) {
            modifiableTrigger.agentRequest(
                agentRequest()
                    .conversationId(agentChannelSummary.conversationIdPath())
                    .message(agentChannelSummary.messagePath())
                    .attachments(agentChannelSummary.attachmentsPath()));

            // Stub triggers carry no properties either, and Task 1's channel-parameter check matches trigger
            // property names EXACTLY (never by dot-segment) - any row key a reply mapping references must be
            // declared here before pairing, the same reason stub action properties are declared below.
            Map<String, String> replyChannelParameters = agentChannelSummary.replyChannelParameters();

            if (modifiableTrigger.getProperties()
                .isEmpty() && !replyChannelParameters.isEmpty()) {

                modifiableTrigger.properties(
                    replyChannelParameters.keySet()
                        .stream()
                        .map(ComponentDsl::string)
                        .toArray(Property[]::new));
            }
        }

        ModifiableAgentChannelDefinition agentChannelDefinition;

        if (agentChannelSummary.replyActionName() == null) {
            agentChannelDefinition = agentChannel(agentChannelSummary.name(), triggerDefinition);
        } else {
            ActionDefinition actionDefinition = componentDefinition.getActions()
                .stream()
                .filter(candidate -> Objects.equals(candidate.getName(), agentChannelSummary.replyActionName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Index entry '%s' agent channel '%s' references unknown action '%s'".formatted(
                        componentDefinition.getName(), agentChannelSummary.name(),
                        agentChannelSummary.replyActionName())));

            // Rebuild the reply descriptor from the summary unconditionally, mirroring the trigger side above:
            // attaching it must not depend on whether the property-declaration guard below ran, or a stub action
            // that already carried properties would never get a reply descriptor at all and later fail pairing
            // with a confusing "no reply descriptor" error instead of a properties-validation one.
            if (actionDefinition instanceof ModifiableActionDefinition modifiableAction) {
                // Stub actions carry no properties, so declare the mapped ones before attaching the reply
                // descriptor - Task 1 validates every mapped name against the action's properties, and a stub
                // with none would fail that check. Stubs never reach detail/execution paths (see the class
                // Javadoc).
                if (modifiableAction.getProperties()
                    .isEmpty()) {

                    modifiableAction.properties(
                        Stream.of(
                            Stream.of(
                                agentChannelSummary.replyMessageProperty(),
                                agentChannelSummary.replyConversationIdProperty(),
                                agentChannelSummary.replyAttachmentsProperty()),
                            agentChannelSummary.replyChannelParameters()
                                .values()
                                .stream(),
                            agentChannelSummary.replyFixedParameters()
                                .keySet()
                                .stream())
                            .flatMap(Function.identity())
                            .filter(Objects::nonNull)
                            .map(propertyName -> string(propertyName.split("\\.")[0]))
                            .distinct()
                            .toList());
                }

                ModifiableAgentReplyDefinition agentReplyDefinition = agentReply()
                    .conversationId(agentChannelSummary.replyConversationIdProperty())
                    .message(agentChannelSummary.replyMessageProperty())
                    .attachments(agentChannelSummary.replyAttachmentsProperty());

                agentChannelSummary.replyChannelParameters()
                    .forEach(agentReplyDefinition::channelParameter);
                agentChannelSummary.replyFixedParameters()
                    .forEach(agentReplyDefinition::fixedParameter);

                modifiableAction.agentReply(agentReplyDefinition);
            }

            agentChannelDefinition = agentChannel(agentChannelSummary.name(), triggerDefinition, actionDefinition);
        }

        return agentChannelDefinition
            .title(agentChannelSummary.title())
            .description(agentChannelSummary.description())
            .approvalChannel(agentChannelSummary.approvalChannelName());
    }

    @SuppressFBWarnings("EI")
    public record Entry(
        String name, int version, @Nullable String title, @Nullable String description, @Nullable String icon,
        @Nullable List<CategorySummary> componentCategories, @Nullable List<String> tags,
        @Nullable ConnectionSummary connection, @Nullable List<ItemSummary> actions,
        @Nullable List<TriggerSummary> triggers, @Nullable List<ClusterElementSummary> clusterElements,
        @Nullable List<ClusterElementTypeSummary> clusterElementTypes,
        @Nullable List<AgentChannelSummary> agentChannels, @Nullable List<String> inputs, String providerClassName,
        String loaderKind) {
    }

    /**
     * Presents a stub component definition as a cluster root, delegating every other call to the stub it wraps.
     */
    private record StubClusterRootComponentDefinition(
        ModifiableComponentDefinition componentDefinition, List<ClusterElementType> clusterElementTypes)
        implements ClusterRootComponentDefinition {

        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return clusterElementTypes;
        }

        @Override
        public List<ActionDefinition> getActions() {
            return componentDefinition.getActions();
        }

        @Override
        public List<AgentChannelDefinition> getAgentChannels() {
            return componentDefinition.getAgentChannels();
        }

        @Override
        public List<ClusterElementDefinition<?>> getClusterElements() {
            return componentDefinition.getClusterElements();
        }

        @Override
        public List<ComponentCategory> getComponentCategories() {
            return componentDefinition.getComponentCategories();
        }

        @Override
        public Optional<ConnectionDefinition> getConnection() {
            return componentDefinition.getConnection();
        }

        @Override
        public boolean getCustomAction() {
            return componentDefinition.getCustomAction();
        }

        @Override
        public Optional<Help> getCustomActionHelp() {
            return componentDefinition.getCustomActionHelp();
        }

        @Override
        public Optional<String> getDescription() {
            return componentDefinition.getDescription();
        }

        @Override
        public Optional<String> getIcon() {
            return componentDefinition.getIcon();
        }

        @Override
        public List<? extends PropertyGroup> getInputs() {
            return componentDefinition.getInputs();
        }

        @Override
        public Map<String, Object> getMetadata() {
            return componentDefinition.getMetadata();
        }

        @Override
        public String getName() {
            return componentDefinition.getName();
        }

        @Override
        public Optional<Resources> getResources() {
            return componentDefinition.getResources();
        }

        @Override
        public List<String> getTags() {
            return componentDefinition.getTags();
        }

        @Override
        public Optional<String> getTitle() {
            return componentDefinition.getTitle();
        }

        @Override
        public List<TriggerDefinition> getTriggers() {
            return componentDefinition.getTriggers();
        }

        @Override
        public Optional<UnifiedApiDefinition> getUnifiedApi() {
            return componentDefinition.getUnifiedApi();
        }

        @Override
        public int getVersion() {
            return componentDefinition.getVersion();
        }
    }

    public record CategorySummary(String name, @Nullable String label) {
    }

    public record ConnectionSummary(int version, boolean authorizationRequired) {
    }

    public record ItemSummary(String name, @Nullable String title, @Nullable String description) {
    }

    public record TriggerSummary(
        String name, @Nullable String title, @Nullable String description, @Nullable String type) {
    }

    public record ClusterElementSummary(
        String name, @Nullable String title, @Nullable String description, String typeName, String typeKey,
        String typeLabel, boolean typeMultipleElements, boolean typeRequired) {
    }

    /**
     * The cluster element types a cluster ROOT component declares — what it accepts as children — as opposed to
     * {@link ClusterElementSummary}, which describes the cluster elements a component contributes to other roots. A
     * component with a non-empty list here is a cluster root, which is what {@code ComponentDefinition.clusterRoot}
     * reports.
     */
    public record ClusterElementTypeSummary(
        String name, String key, String label, boolean multipleElements, boolean required) {
    }

    /**
     * Carries the full request/reply binding for an agent channel, not just its identity, because the index serves the
     * components-list stub without loading the real component. A stub whose channel carried only a name would let the
     * AI Agent workflow generator wire the channel's expressions wrong — worse than the channel not appearing on the
     * stub at all.
     */
    @SuppressFBWarnings("EI")
    public record AgentChannelSummary(
        String name, @Nullable String title, @Nullable String description, String triggerName,
        @Nullable String replyActionName, @Nullable String approvalChannelName, String conversationIdPath,
        String messagePath, @Nullable String attachmentsPath, @Nullable String replyMessageProperty,
        @Nullable String replyConversationIdProperty, @Nullable String replyAttachmentsProperty,
        Map<String, String> replyChannelParameters, Map<String, Object> replyFixedParameters) {
    }
}
