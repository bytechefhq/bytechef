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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.AgentReplyDefinition;
import com.bytechef.component.definition.AgentRequestDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
public class ComponentIndexTest {

    @TempDir
    Path tempDir;

    private Path writeIndex(String directoryName, String json) throws Exception {
        Path directoryPath = tempDir.resolve(directoryName);

        Path indexPath = directoryPath.resolve(ComponentIndex.RESOURCE_PATH);

        Files.createDirectories(Objects.requireNonNull(indexPath.getParent()));
        Files.writeString(indexPath, json);

        return directoryPath;
    }

    private static URLClassLoader classLoaderOf(Path... roots) throws Exception {
        URL[] urls = new URL[roots.length];

        for (int i = 0; i < roots.length; i++) {
            urls[i] = roots[i].toUri()
                .toURL();
        }

        return new URLClassLoader(urls, null);
    }

    @Test
    public void testLoadReturnsEmptyWhenNoResourceExists() throws Exception {
        try (URLClassLoader classLoader = classLoaderOf(tempDir)) {
            assertThat(ComponentIndex.load(classLoader)).isEmpty();
        }
    }

    @Test
    public void testLoadReturnsEmptyOnMalformedJson() throws Exception {
        Path root = writeIndex("broken", "this is not json");

        try (URLClassLoader classLoader = classLoaderOf(root)) {
            // Broken index must degrade to empty (registry then falls back to full loading), never throw.
            assertThat(ComponentIndex.load(classLoader)).isEmpty();
        }
    }

    @Test
    public void testLoadMergesMultipleResourcesAndDeduplicatesByNameAndVersion() throws Exception {
        String first = """
            {"entries":[
              {"name":"alpha","version":1,"title":"Alpha (first)","providerClassName":"a.AlphaHandler",
               "loaderKind":"default"},
              {"name":"beta","version":1,"title":"Beta","providerClassName":"b.BetaHandler","loaderKind":"default"}
            ]}""";
        String second = """
            {"entries":[
              {"name":"alpha","version":1,"title":"Alpha (second)","providerClassName":"a.AlphaHandler",
               "loaderKind":"default"},
              {"name":"gamma","version":2,"title":"Gamma","providerClassName":"g.GammaHandler","loaderKind":"jdbc"}
            ]}""";

        Path firstRoot = writeIndex("first", first);
        Path secondRoot = writeIndex("second", second);

        try (URLClassLoader classLoader = classLoaderOf(firstRoot, secondRoot)) {
            Optional<ComponentIndex> componentIndexOptional = ComponentIndex.load(classLoader);

            assertThat(componentIndexOptional).isPresent();

            ComponentIndex componentIndex = componentIndexOptional.orElseThrow();

            assertThat(componentIndex.entries()).hasSize(3);

            // Duplicate name/version pairs keep the first occurrence.
            ComponentIndex.Entry alphaEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "alpha".equals(entry.name()))
                .findFirst()
                .orElseThrow();

            assertThat(alphaEntry.title()).isEqualTo("Alpha (first)");
            assertThat(componentIndex.entries())
                .extracting(ComponentIndex.Entry::name)
                .containsExactlyInAnyOrder("alpha", "beta", "gamma");
        }
    }

    @Test
    public void testAgentChannelsRoundTripThroughStub() {
        // Every path/property below is dotted and non-default (none equals AgentChannelDefinition.MESSAGE,
        // CONVERSATION_ID or ATTACHMENTS) so a silently dropped binding field cannot pass by coincidentally
        // matching a default value.
        ModifiableTriggerDefinition triggerDefinition = trigger("newAiAgentMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            // A nested output schema matching the dotted paths below, rather than the flat contract: ComponentDsl
            // walks a request path to its last segment when the trigger declares a schema, and the contract's
            // 'message' is a string that "message.chat.id" cannot descend into.
            .output(
                outputSchema(
                    object()
                        .properties(
                            object("message")
                                .properties(
                                    string("text"),
                                    array("files").items(fileEntry()),
                                    object("chat")
                                        .properties(string("id"))))))
            // Declared so the rebuilt stub's channel-parameter row key ("channelId" below) has a real trigger
            // property to match exactly, per ComponentDsl.validateChannelParameterKeys.
            .properties(string("channelId"))
            .agentRequest(
                agentRequest()
                    .conversationId("message.chat.id")
                    .message("message.text")
                    .attachments("message.files"));
        ModifiableActionDefinition actionDefinition = action("sendAiAgentReply")
            .properties(string("response"))
            .agentReply(
                agentReply()
                    .message("response.text")
                    .conversationId("response.chatId")
                    .attachments("response.files")
                    .channelParameter("channelId", "response.channelId")
                    .fixedParameter("response.locale", "en-US"));
        ComponentDefinition componentDefinition = component("acme")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(agentChannel("acme", triggerDefinition, actionDefinition).title("Acme"));

        ComponentIndex.Entry entry = ComponentIndexGenerator.toEntry(componentDefinition, "Provider", "service-loader");
        ComponentDefinition stub = ComponentIndex.toStubComponentDefinition(entry);

        assertThat(stub.getAgentChannels()).hasSize(1);

        AgentChannelDefinition stubAgentChannelDefinition = stub.getAgentChannels()
            .getFirst();

        assertThat(stubAgentChannelDefinition.getName()).isEqualTo("acme");

        TriggerDefinition stubTriggerDefinition = stubAgentChannelDefinition.getTrigger();

        assertThat(stubTriggerDefinition.getName()).isEqualTo("newAiAgentMessage");

        AgentRequestDefinition stubAgentRequestDefinition = stubTriggerDefinition.getAgentRequestDefinition()
            .orElseThrow();

        assertThat(stubAgentRequestDefinition.getConversationIdPath()).isEqualTo("message.chat.id");
        assertThat(stubAgentRequestDefinition.getMessagePath()).isEqualTo("message.text");
        assertThat(stubAgentRequestDefinition.getAttachmentsPath()).contains("message.files");

        ActionDefinition stubReplyActionDefinition = stubAgentChannelDefinition.getReplyAction()
            .orElseThrow();

        assertThat(stubReplyActionDefinition.getName()).isEqualTo("sendAiAgentReply");

        AgentReplyDefinition stubAgentReplyDefinition = stubReplyActionDefinition.getAgentReplyDefinition()
            .orElseThrow();

        assertThat(stubAgentReplyDefinition.getMessageProperty()).isEqualTo("response.text");
        assertThat(stubAgentReplyDefinition.getConversationIdProperty()).contains("response.chatId");
        assertThat(stubAgentReplyDefinition.getAttachmentsProperty()).contains("response.files");
        assertThat(stubAgentReplyDefinition.getChannelParameters()).containsEntry("channelId", "response.channelId");
        assertThat(stubAgentReplyDefinition.getFixedParameters()).containsEntry("response.locale", "en-US");
    }
}
