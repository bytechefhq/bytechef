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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Everything the agent workflow generator needs about one channel key, resolved once per generation from the component
 * registry (see {@code AgentChannelResolver} in {@code automation-ai-agent-service}) rather than from a hand-maintained
 * table in this module — a component declares its own channel through the SDK's {@code agentChannel(...)} DSL, and this
 * record is that declaration projected onto what the generator consumes.
 * <p>
 * Lives in {@code -api} rather than next to the resolver in {@code -service} because {@code AiAgentFacade} returns a
 * list of these ({@link com.bytechef.automation.ai.agent.facade.AiAgentFacade#getAgentChannelDefinitions()}) and the
 * GraphQL module compiles against {@code -api} alone.
 *
 * @param name                       the channel key, stored verbatim in {@code ai_agent_channel.channel_type}
 * @param title                      the channel's display title, never blank — the resolver falls back to the owning
 *                                   component's title and then to its name, since the client renders this string
 *                                   directly and a missing declaration would otherwise surface a raw lowercase
 *                                   component name as a channel's label
 * @param description                the channel's display description, or {@code null} when the component declared none
 * @param icon                       the owning component's icon, or {@code null} when it declares none
 * @param triggerType                {@code "<component>/v<version>/<triggerName>"}
 * @param replyActionType            {@code "<component>/v<version>/<actionName>"}, or {@code null} when the channel has
 *                                   no reply action (a schedule answers nobody)
 * @param connectionRequired         whether the owning component needs a design-time connection — the trigger and its
 *                                   paired reply action share one
 * @param triggerParameters          fixed parameters the channel declaration pins onto the trigger node ({@code
 *                                   workflowCall}'s {@code inputSchema})
 * @param triggerPropertyDefaults    declared trigger property default values, by property name; only properties that
 *                                   actually declare one appear
 * @param triggerPropertyNames       every declared trigger property name — the allow-list restricting which
 *                                   {@code ai_agent_channel.parameters} keys reach the generated trigger node, so
 *                                   UI-only row keys (a schedule row's {@code prompt}/{@code name}) stay on the row
 * @param requiredReplyPropertyNames the reply action's REQUIRED top-level property names; publish validation refuses an
 *                                   agent whose channel row leaves one of these unfed through
 *                                   {@link Binding#replyChannelParameters()}, since the generator omits an unset mapped
 *                                   parameter and the reply would fail at run time instead. Empty when the channel has
 *                                   no reply action
 * @param binding                    where the contract's fields live on each end of this channel
 * @param approvalDelivery           where an approval request reaches a human on this channel, or {@code null} when the
 *                                   channel cannot carry one
 * @author Ivica Cardic
 */
// The collection components below are already immutable — insertion-ordered unmodifiable views rather than
// Map.copyOf/Set.copyOf, for the determinism reason the compact constructor documents. SpotBugs recognizes only the
// latter as immutable, so EI_EXPOSE_REP on the generated accessors is a false positive here.
@SuppressFBWarnings("EI")
public record ResolvedAgentChannel(
    String name, String title, @Nullable String description, @Nullable String icon, String triggerType,
    @Nullable String replyActionType, boolean connectionRequired, Map<String, Object> triggerParameters,
    Map<String, Object> triggerPropertyDefaults, Set<String> triggerPropertyNames,
    Set<String> requiredReplyPropertyNames, Binding binding, @Nullable ApprovalDelivery approvalDelivery) {

    public ResolvedAgentChannel {
        // Defensive immutable copies so a resolved channel cannot be mutated through the caller's collections
        // (SpotBugs EI_EXPOSE_REP/REP2). Insertion-ordered rather than Map.copyOf/Set.copyOf: the generator writes
        // these straight into the workflow definition, and the immutable factories' iteration order is salted per JVM
        // run, which would make a regenerated workflow differ from the stored one byte-wise for no reason.
        triggerParameters = orderedCopyOf(triggerParameters);
        triggerPropertyDefaults = orderedCopyOf(triggerPropertyDefaults);
        triggerPropertyNames = Collections.unmodifiableSet(new LinkedHashSet<>(triggerPropertyNames));
        requiredReplyPropertyNames = Collections.unmodifiableSet(new LinkedHashSet<>(requiredReplyPropertyNames));
    }

    /**
     * Where the contract's fields live on each end of one channel, copied from the component's own request/reply
     * descriptors. This is what makes the generator component-agnostic: telegram's {@code "message.chat.id"} and chat's
     * {@code "conversationId"} take the same code path.
     * <p>
     * {@code conversationIdPath} and {@code messagePath} are non-null for every channel a component declares — the SDK
     * validates both at construction time. They are nullable here for the single synthesized non-channel the resolver
     * produces, {@code schedule}, which configures its prompt and conversation key on the row instead of receiving them
     * from a trigger; the generator's one schedule branch fills both before either path would be read, so a null here
     * fails loudly rather than silently emitting an expression that reads nothing.
     *
     * @param conversationIdPath          path into the trigger's output holding the conversation id
     * @param messagePath                 path into the trigger's output holding the incoming text
     * @param attachmentsPath             path into the trigger's output holding incoming files, or {@code null} when
     *                                    the channel carries none — the generator then wires an empty array
     * @param replyMessageProperty        the reply action property the agent's answer is written to; a dotted name
     *                                    descends into a nested (dynamic) property map
     * @param replyConversationIdProperty the reply action property addressing the answer, or {@code null} when the
     *                                    reply is a synchronous response
     * @param replyAttachmentsProperty    the reply action property carrying outgoing files, or {@code null};
     *                                    deliberately left unwired today, since the agent node's output is a bare
     *                                    string
     * @param replyChannelParameters      row parameter name to reply action property name, for values configured on the
     *                                    channel row (twilio's {@code number} becomes the reply's {@code From})
     * @param replyFixedParameters        reply action parameters pinned by the declaration (twilio's
     *                                    {@code useTemplate = false}, {@code workflowCall}'s {@code outputSchema})
     */
    @SuppressFBWarnings("EI")
    public record Binding(
        @Nullable String conversationIdPath, @Nullable String messagePath, @Nullable String attachmentsPath,
        @Nullable String replyMessageProperty, @Nullable String replyConversationIdProperty,
        @Nullable String replyAttachmentsProperty, Map<String, String> replyChannelParameters,
        Map<String, Object> replyFixedParameters) {

        public Binding {
            // Insertion-ordered, for the same reason ResolvedAgentChannel's own copies are.
            replyChannelParameters = orderedCopyOf(replyChannelParameters);
            replyFixedParameters = orderedCopyOf(replyFixedParameters);
        }
    }

    /**
     * The {@code APPROVAL_CHANNELS} cluster element an approval request is delivered through for this channel.
     *
     * @param componentName the component owning that element, which is the channel's own component
     * @param elementName   which of its elements to use — twilio and infobip both default to {@code sms} while their
     *                      agent channels are WhatsApp, so the channel declaration names the element explicitly
     */
    public record ApprovalDelivery(String componentName, String elementName) {
    }

    public String componentName() {
        return triggerType.substring(0, triggerType.indexOf('/'));
    }

    /**
     * Whether this channel's row has anything to configure beyond its connection — the trigger declares at least one
     * property the channel declaration does not pin.
     * <p>
     * Derived rather than stored, from the two collections this record already carries, so it cannot drift from them.
     * It exists because the client used {@link #connectionRequired()} as a stand-in for "worth configuring", which
     * happens to be right for every channel that needs a connection and wrong for {@code chat}, whose {@code mode}
     * property has no connection behind it — and would be wrong for any future channel of that shape.
     * <p>
     * A PINNED parameter is not configurable: the channel declaration fixes it and a row cannot change it, which is why
     * {@code workflowCall} — whose only property is the {@code inputSchema} it pins — reports {@code false}.
     *
     * @return {@code true} when at least one declared trigger property is left for the row to set
     */
    public boolean propertiesConfigurable() {
        return triggerPropertyNames.stream()
            .anyMatch(propertyName -> !triggerParameters.containsKey(propertyName));
    }

    private static <V> Map<String, V> orderedCopyOf(Map<String, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
