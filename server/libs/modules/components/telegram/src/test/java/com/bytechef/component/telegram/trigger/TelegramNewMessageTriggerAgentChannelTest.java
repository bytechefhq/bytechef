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

package com.bytechef.component.telegram.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.AgentReplyDefinition;
import com.bytechef.component.definition.AgentRequestDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.telegram.TelegramComponentHandler;
import com.bytechef.component.telegram.action.TelegramSendMessageAction;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Exercises the {@code telegram} agent channel's derivation rather than its declaration.
 * <p>
 * {@code ComponentDsl} validates only the FIRST segment of a request path against the trigger's output, so
 * {@code "message.anythingAtAll"} is accepted at construction time as readily as {@code "message.chat.id"}. Telegram is
 * the first channel whose paths are dotted, hence the first for which the rest of the path can drift unnoticed. This
 * test closes that gap by walking each declared path segment by segment through the trigger's real output schema.
 * <p>
 * The expected paths and property names are spelled as literals rather than taken from {@code TelegramConstants}, so
 * that the test checks the production declaration instead of restating it.
 *
 * @author Ivica Cardic
 */
class TelegramNewMessageTriggerAgentChannelTest {

    private static final TriggerDefinition TRIGGER_DEFINITION = TelegramNewMessageTrigger.TRIGGER_DEFINITION;

    @Test
    void testConversationIdPathResolvesAgainstTheTriggersDeclaredOutput() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        String conversationIdPath = agentRequestDefinition.getConversationIdPath();

        assertEquals(
            "message.chat.id", conversationIdPath,
            "the conversation identifier must keep addressing the chat the message arrived in, which is also the "
                + "address the reply is sent back to");
        assertTrue(
            resolvePath(conversationIdPath)
                .isPresent(),
            "conversationIdPath '%s' must resolve against the trigger's declared output"
                .formatted(conversationIdPath));
    }

    @Test
    void testMessagePathResolvesAgainstTheTriggersDeclaredOutput() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        String messagePath = agentRequestDefinition.getMessagePath();

        assertEquals("message.text", messagePath);
        assertTrue(
            resolvePath(messagePath)
                .isPresent(),
            "messagePath '%s' must resolve against the trigger's declared output".formatted(messagePath));
    }

    /**
     * Negative control. Without it the resolver could be vacuously true and both path assertions above would pass for
     * the wrong reason.
     */
    @Test
    void testAnUndeclaredPathDoesNotResolve() {
        assertFalse(
            resolvePath("message.chat.somethingElse")
                .isPresent(),
            "a path naming a field the trigger does not declare must not resolve");
        assertFalse(
            resolvePath("message.text.id")
                .isPresent(),
            "a path descending into a leaf must not resolve");
    }

    /**
     * The conversation identifier is declared an INTEGER while the agent channel contract calls for a string. That is
     * pre-existing behaviour rather than a regression: the value is read through an expression, which stringifies it,
     * exactly as today's {@code ${__NODE__.message.chat.id}} mapping does. Pinned here so the deviation stays visible
     * instead of being rediscovered, and so a future contract-level type check knows which channel it breaks first.
     */
    @Test
    void testConversationIdIsDeclaredAnIntegerAndIsStringifiedByTheExpression() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        BaseProperty conversationIdProperty = resolvePath(agentRequestDefinition.getConversationIdPath())
            .orElseThrow();

        Property property = assertInstanceOf(Property.class, conversationIdProperty);

        assertEquals(
            Property.Type.INTEGER, property.getType(),
            "Telegram declares the chat identifier an integer; the contract's conversationId is a string, and the "
                + "expression that reads it is what stringifies the value");
    }

    /**
     * Telegram carries no attachments into the agent, and an absent {@code attachmentsPath} is how a channel says so —
     * the generator then wires the literal {@code []}, which is exactly today's mapping. This is a decision, not the
     * oversight it was for {@code chat} and {@code workflowCall}, so it is asserted together with the fact that makes
     * it correct: the trigger's output declares no attachments field to bind.
     */
    @Test
    void testAttachmentsAreDeliberatelyUnbound() {
        AgentRequestDefinition agentRequestDefinition = getAgentRequestDefinition();

        assertTrue(
            agentRequestDefinition.getAttachmentsPath()
                .isEmpty(),
            "telegram binds no attachmentsPath, so the generator wires [] as it does today");

        assertFalse(
            getMessagePropertyNames().contains(AgentChannelDefinition.ATTACHMENTS),
            "the trigger's message output must declare no attachments field, otherwise the unbound attachmentsPath "
                + "would be dropping something the channel actually carries");
    }

    @Test
    void testReplyPropertiesAreDeclaredBySendMessage() {
        AgentReplyDefinition agentReplyDefinition = TelegramSendMessageAction.ACTION_DEFINITION
            .getAgentReplyDefinition()
            .orElseThrow();

        assertEquals(
            "chat_id", agentReplyDefinition.getConversationIdProperty()
                .orElseThrow(
                    () -> new AssertionError(
                        "the reply must be addressed: a Telegram message goes to a chat, it is not a response down an "
                            + "open request")));
        assertEquals("text", agentReplyDefinition.getMessageProperty());
        assertTrue(
            agentReplyDefinition.getAttachmentsProperty()
                .isEmpty(),
            "sendMessage declares no attachments property, matching the unbound request half");

        Set<String> propertyNames = TelegramSendMessageAction.ACTION_DEFINITION.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());

        assertTrue(propertyNames.contains("chat_id"), "the reply's conversationId property must really be declared");
        assertTrue(propertyNames.contains("text"), "the reply's message property must really be declared");
    }

    @Test
    void testChannelPairsTheTriggerAndTheReplyActionUnderTheStoredKey() {
        AgentChannelDefinition agentChannelDefinition = getAgentChannelDefinition();

        assertEquals("telegram", agentChannelDefinition.getName(), "the stored channel key must not drift");
        assertEquals(Optional.of("Telegram"), agentChannelDefinition.getTitle());
        assertEquals(
            Optional.of("telegram"), agentChannelDefinition.getApprovalChannelName(),
            "approvals reach the user through the component's own telegram approval channel element");

        TriggerDefinition triggerDefinition = agentChannelDefinition.getTrigger();

        assertEquals(TRIGGER_DEFINITION.getName(), triggerDefinition.getName());

        ActionDefinition actionDefinition = agentChannelDefinition.getReplyAction()
            .orElseThrow();

        assertEquals(TelegramSendMessageAction.ACTION_DEFINITION.getName(), actionDefinition.getName());

        assertEquals(
            Map.of(), agentChannelDefinition.getTriggerParameters(),
            "the trigger needs no pinned parameters: it emits Telegram's own payload whatever it is configured with");
    }

    private static AgentChannelDefinition getAgentChannelDefinition() {
        TelegramComponentHandler telegramComponentHandler = new TelegramComponentHandler();

        ComponentDefinition componentDefinition = telegramComponentHandler.getDefinition();

        List<AgentChannelDefinition> agentChannelDefinitions = componentDefinition.getAgentChannels();

        assertEquals(1, agentChannelDefinitions.size(), "the telegram component declares exactly one agent channel");

        return agentChannelDefinitions.getFirst();
    }

    private static AgentRequestDefinition getAgentRequestDefinition() {
        return TRIGGER_DEFINITION.getAgentRequestDefinition()
            .orElseThrow(() -> new AssertionError("the trigger must declare an agentRequest descriptor"));
    }

    private static Set<String> getMessagePropertyNames() {
        BaseProperty messageProperty = resolvePath("message").orElseThrow();

        BaseObjectProperty<?> objectProperty = assertInstanceOf(BaseObjectProperty.class, messageProperty);

        return objectProperty.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Walks a dotted path through the trigger's declared output, one segment at a time. {@code ComponentDsl} checks
     * only the first segment, so this is the only thing standing between a mistyped nested field and an agent run that
     * silently reads nothing.
     */
    private static Optional<BaseProperty> resolvePath(String path) {
        BaseProperty currentProperty = TRIGGER_DEFINITION.getOutputDefinition()
            .flatMap(OutputDefinition::getOutputResponse)
            .map(OutputResponse::getOutputSchema)
            .orElseThrow(() -> new AssertionError("the trigger must declare a static output schema"));

        for (String segment : path.split("\\.")) {
            if (!(currentProperty instanceof BaseObjectProperty<?> objectProperty)) {
                return Optional.empty();
            }

            Optional<BaseProperty> nextProperty = objectProperty.getProperties()
                .stream()
                .filter(property -> segment.equals(property.getName()))
                .map(BaseProperty.class::cast)
                .findFirst();

            if (nextProperty.isEmpty()) {
                return Optional.empty();
            }

            currentProperty = nextProperty.get();
        }

        return Optional.of(currentProperty);
    }
}
