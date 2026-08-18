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

package com.bytechef.automation.ai.agent.channel;

import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.AgentChannelDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves a stored {@code ai_agent_channel.channel_type} against the component registry, projecting the component's
 * own {@code agentChannel(...)} declaration plus the paired trigger's and reply action's property definitions onto a
 * {@link ResolvedAgentChannel} the workflow generator can render without naming any component.
 * <p>
 * <b>The schedule exception lives here, once.</b> A schedule is not a channel — it has nobody on the other end, no
 * reply action and nothing arriving from outside (see
 * {@code docs/superpowers/specs/2026-08-17-sdk-agent-channels-design.md}, "What is not a channel"), so the registry
 * cannot supply it and the {@code schedule} component declares nothing. Rather than branching in every generator
 * method, this resolver synthesizes an entry for it: the existing {@code schedule/v1/cron} trigger, no reply action,
 * and {@code triggerPropertyNames}/{@code triggerPropertyDefaults} read from that trigger's REAL property definitions,
 * so the row-parameter allow-list stays correct without hardcoding {@code expression}/{@code timezone}. Its
 * {@link ResolvedAgentChannel.Binding} carries null paths: the generator fills a schedule's text and conversation id
 * from the channel row before any path would be consulted.
 *
 * @author Ivica Cardic
 */
@Component
public class AgentChannelResolver {

    private static final String SCHEDULE_COMPONENT_NAME = "schedule";
    private static final int SCHEDULE_COMPONENT_VERSION = 1;
    private static final String SCHEDULE_CRON_TRIGGER_NAME = "cron";

    /**
     * A schedule receives nothing, so there is no request path to bind and no reply to address — see this class's
     * javadoc.
     */
    private static final ResolvedAgentChannel.Binding SCHEDULE_BINDING = new ResolvedAgentChannel.Binding(
        null, null, null, null, null, null, Map.of(), Map.of());

    private final ComponentDefinitionService componentDefinitionService;

    public AgentChannelResolver(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    /**
     * @return the resolved channel, or {@link Optional#empty()} when no component declares {@code channelName}
     */
    public Optional<ResolvedAgentChannel> resolve(String channelName) {
        if (AiAgentChannelType.SCHEDULE.equals(channelName)) {
            return Optional.of(resolveSchedule());
        }

        return componentDefinitionService.fetchAgentChannelDefinition(channelName)
            .map(this::toResolvedAgentChannel);
    }

    /**
     * Every channel an agent can be reached through, plus the synthesized {@code schedule} entry the registry cannot
     * supply — the client renders one card per entry, and a schedule card without its metadata would have nowhere else
     * to come from. {@code schedule} is a reserved key: it replaces (rather than duplicates) a registry entry of the
     * same name, matching {@link #resolve(String)}'s own precedence.
     * <p>
     * Registry order, with the synthesized entry LAST — the order reaches the client verbatim through
     * {@code aiAgentChannelDefinitions}, and the one entry that is not a channel does not belong at the head of a list
     * of channels. The client keys off the {@code schedule}/{@code pinned} flags rather than position, so this is a
     * presentation default, not a contract.
     */
    public List<ResolvedAgentChannel> resolveAll() {
        Map<String, ResolvedAgentChannel> resolvedChannels = new LinkedHashMap<>();

        for (AgentChannelDefinition agentChannelDefinition : componentDefinitionService.getAgentChannelDefinitions()) {
            resolvedChannels.put(agentChannelDefinition.getName(), toResolvedAgentChannel(agentChannelDefinition));
        }

        resolvedChannels.put(AiAgentChannelType.SCHEDULE, resolveSchedule());

        return List.copyOf(resolvedChannels.values());
    }

    private ResolvedAgentChannel resolveSchedule() {
        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            SCHEDULE_COMPONENT_NAME, SCHEDULE_COMPONENT_VERSION);
        TriggerDefinition triggerDefinition = getTriggerDefinition(componentDefinition, SCHEDULE_CRON_TRIGGER_NAME);

        return new ResolvedAgentChannel(
            AiAgentChannelType.SCHEDULE, title(null, componentDefinition), componentDefinition.getDescription(),
            componentDefinition.getIcon(),
            nodeType(SCHEDULE_COMPONENT_NAME, SCHEDULE_COMPONENT_VERSION, SCHEDULE_CRON_TRIGGER_NAME), null,
            componentDefinition.isConnectionRequired(), Map.of(),
            triggerPropertyDefaults(triggerDefinition), propertyNames(triggerDefinition.getProperties()), Set.of(),
            SCHEDULE_BINDING, null);
    }

    private ResolvedAgentChannel toResolvedAgentChannel(AgentChannelDefinition agentChannelDefinition) {
        String componentName = agentChannelDefinition.getComponentName();
        int componentVersion = agentChannelDefinition.getComponentVersion();

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);
        TriggerDefinition triggerDefinition = getTriggerDefinition(
            componentDefinition, agentChannelDefinition.getTriggerName());

        String replyActionName = agentChannelDefinition.getReplyActionName();
        String replyActionType = null;
        Set<String> requiredReplyPropertyNames = Set.of();

        if (replyActionName != null) {
            replyActionType = nodeType(componentName, componentVersion, replyActionName);
            requiredReplyPropertyNames = requiredPropertyNames(
                getActionDefinition(componentDefinition, replyActionName));
        }

        String approvalChannelName = agentChannelDefinition.getApprovalChannelName();

        return new ResolvedAgentChannel(
            agentChannelDefinition.getName(), title(agentChannelDefinition.getTitle(), componentDefinition),
            agentChannelDefinition.getDescription(), componentDefinition.getIcon(),
            nodeType(componentName, componentVersion, agentChannelDefinition.getTriggerName()), replyActionType,
            componentDefinition.isConnectionRequired(), agentChannelDefinition.getTriggerParameters(),
            triggerPropertyDefaults(triggerDefinition), propertyNames(triggerDefinition.getProperties()),
            requiredReplyPropertyNames, toBinding(agentChannelDefinition),
            approvalChannelName == null
                ? null
                : new ResolvedAgentChannel.ApprovalDelivery(componentName, approvalChannelName));
    }

    private static ResolvedAgentChannel.Binding toBinding(AgentChannelDefinition agentChannelDefinition) {
        return new ResolvedAgentChannel.Binding(
            agentChannelDefinition.getConversationIdPath(), agentChannelDefinition.getMessagePath(),
            agentChannelDefinition.getAttachmentsPath(), agentChannelDefinition.getReplyMessageProperty(),
            agentChannelDefinition.getReplyConversationIdProperty(),
            agentChannelDefinition.getReplyAttachmentsProperty(),
            agentChannelDefinition.getReplyChannelParameters(), agentChannelDefinition.getReplyFixedParameters());
    }

    private static ActionDefinition getActionDefinition(
        ComponentDefinition componentDefinition, String actionName) {

        return componentDefinition.getActions()
            .stream()
            .filter(actionDefinition -> Objects.equals(actionDefinition.getName(), actionName))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException(
                    "Component " + componentDefinition.getName() + " declares no action " + actionName));
    }

    private static TriggerDefinition getTriggerDefinition(
        ComponentDefinition componentDefinition, String triggerName) {

        return componentDefinition.getTriggers()
            .stream()
            .filter(triggerDefinition -> Objects.equals(triggerDefinition.getName(), triggerName))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException(
                    "Component " + componentDefinition.getName() + " declares no trigger " + triggerName));
    }

    private static Set<String> propertyNames(List<? extends Property> properties) {
        Set<String> names = new LinkedHashSet<>();

        for (Property property : properties) {
            names.add(property.getName());
        }

        return names;
    }

    /**
     * Top-level only, deliberately: a nested target such as {@code response.message} lives inside a
     * {@code dynamicProperties} map whose members are not statically known, so its requiredness cannot be read here and
     * publish validation leaves such a mapping alone rather than guessing from the outer property.
     */
    private static Set<String> requiredPropertyNames(ActionDefinition actionDefinition) {
        Set<String> names = new LinkedHashSet<>();

        for (Property property : actionDefinition.getProperties()) {
            if (property.getRequired()) {
                names.add(property.getName());
            }
        }

        return names;
    }

    private static Map<String, Object> triggerPropertyDefaults(TriggerDefinition triggerDefinition) {
        Map<String, Object> defaultValues = new LinkedHashMap<>();

        for (Property property : triggerDefinition.getProperties()) {
            if (property instanceof ValueProperty<?> valueProperty && valueProperty.getDefaultValue() != null) {
                defaultValues.put(property.getName(), valueProperty.getDefaultValue());
            }
        }

        return defaultValues;
    }

    /**
     * The channel's display title, made total: the declaration's own title, else the owning component's, else its name.
     * Both {@code agentChannel(...).title(...)} and {@code component(...).title(...)} are optional in the SDK, and the
     * client renders this string as a channel's label — a null here would fall through to a raw lowercase component
     * name, which is exactly the bug the {@code aiAgentChannelDefinitions} query replaces.
     */
    private static String title(@Nullable String declaredTitle, ComponentDefinition componentDefinition) {
        if (declaredTitle != null && !declaredTitle.isBlank()) {
            return declaredTitle;
        }

        String componentTitle = componentDefinition.getTitle();

        if (componentTitle != null && !componentTitle.isBlank()) {
            return componentTitle;
        }

        return componentDefinition.getName();
    }

    private static String nodeType(String componentName, int componentVersion, String operationName) {
        return componentName + "/v" + componentVersion + "/" + operationName;
    }
}
