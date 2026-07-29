/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Pin the failure-mode coverage for {@link NonEmptyMessagesAdvisor}. Every test names the wire-level Anthropic 400 it
 * prevents and which Spring AI bug surfaces the problem.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class NonEmptyMessagesAdvisorTest {

    @Test
    void testPassesThroughWhenAllMessagesAreNonEmpty() {
        // Hot path: every message has content. The advisor must NOT mutate the request — even an
        // identity-preserving copy would defeat downstream identity comparisons in the chain.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Hello"),
            new AssistantMessage("Hi there"),
            new UserMessage("How are you?")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        ChatClientResponse response = dispatch(advisor, request);

        assertThat(response).isNotNull();
    }

    @Test
    void testSubstitutesPlaceholderForEmptyUserMessage() {
        // Defensive: a UserMessage with blank text shouldn't happen in practice (users always type text)
        // but the advisor must still substitute a placeholder so Anthropic doesn't reject the call.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(new UserMessage("Hello"), new AssistantMessage("Hi"), new UserMessage("")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);
        assertThat(patched.get(2)
            .getText()).isEqualTo("no content");
    }

    @Test
    void testStripsToolResponseMessageWithEmptyResponsesList() {
        // EXACT reproduction of the production failure mode. Spring AI's JdbcChatMemoryRepository load
        // path: `case TOOL -> ToolResponseMessage.builder().responses(List.of()).build();` — every TOOL
        // row replayed from memory has zero responses. The advisor must strip these entirely; substituting
        // a placeholder here would feed the LLM hallucinated "tool returned nothing" history that contradicts
        // what actually happened on the prior turn.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        ToolResponseMessage staleTool = ToolResponseMessage.builder()
            .responses(List.of())
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Previous prompt"),
            new AssistantMessage("Previous reply"),
            staleTool,
            new UserMessage("Current prompt")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Previous prompt");
        assertThat(patched.get(2)
            .getText()).isEqualTo("Current prompt");
    }

    @Test
    void testStripsAssistantMessageWithEmptyTextAndNoToolCalls() {
        // Companion to the TOOL-strip path: a chat-memory-replayed AssistantMessage carries empty text
        // (the LLM's first iteration emitted only tool_use, which JdbcChatMemoryRepository doesn't persist)
        // AND no tool_calls (also not persisted). The combination uniquely identifies stale-history; current-
        // turn assistants always have at least tool_calls populated.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Prev"),
            new AssistantMessage(""),
            new UserMessage("Current")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Prev");
        assertThat(patched.get(1)
            .getText()).isEqualTo("Current");
    }

    @Test
    void testKeepsAssistantWithEmptyTextButPresentToolCalls() {
        // Mid-turn assistant: the LLM emitted only tool_use, no text. Spring AI builds the AssistantMessage
        // with empty text but non-empty toolCalls. This is current-turn state, NOT chat-memory replay — must
        // be preserved because Anthropic needs the tool_use blocks to match the next tool_result message.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage currentTurn = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("call-1", "function", "searchComponents", "{}")))
            .build();

        Prompt prompt = new Prompt(List.of(new UserMessage("Find Slack"), currentTurn));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(1)).isSameAs(currentTurn);
    }

    @Test
    void testStripsToolResponseMessageWithAllBlankResponseData() {
        // Defensive: a ToolResponseMessage with a populated responses list where every responseData is
        // empty/whitespace. Treat the same as the all-empty list case — strip rather than substitute, since
        // the data is unrecoverable and substitution would feed the LLM stale "tool returned nothing" rows.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        ToolResponseMessage staleTool = ToolResponseMessage.builder()
            .responses(List.of(
                new ToolResponseMessage.ToolResponse("call-1", "searchComponents", ""),
                new ToolResponseMessage.ToolResponse("call-2", "listConnections", "   ")))
            .build();

        Prompt prompt = new Prompt(List.of(new UserMessage("Q"), new AssistantMessage("A"), staleTool));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
    }

    @Test
    void testPatchesOnlyEmptyResponseDataWhenMixed() {
        // Mixed-response messages: a ToolResponseMessage can carry several ToolResponse entries (parallel
        // tool calls in current turn). Only the empty ones get placeholder substitution; non-empty siblings
        // pass through unchanged so the LLM still sees its actual tool results.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        ToolResponseMessage toolMessage = ToolResponseMessage.builder()
            .responses(List.of(
                new ToolResponseMessage.ToolResponse("call-1", "good", "{\"ok\":true}"),
                new ToolResponseMessage.ToolResponse("call-2", "bad", ""),
                new ToolResponseMessage.ToolResponse("call-3", "alsoGood", "{\"ok\":true}")))
            .build();

        Prompt prompt = new Prompt(List.of(new UserMessage("Q"), new AssistantMessage("A"), toolMessage));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);

        ToolResponseMessage finalToolMessage = (ToolResponseMessage) patched.get(2);

        assertThat(finalToolMessage.getResponses()).hasSize(3);
        assertThat(finalToolMessage.getResponses()
            .get(0)
            .responseData()).isEqualTo("{\"ok\":true}");
        assertThat(finalToolMessage.getResponses()
            .get(1)
            .responseData()).isEqualTo("{\"result\":\"no content\"}");
        assertThat(finalToolMessage.getResponses()
            .get(2)
            .responseData()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void testStripsAdjacentStaleAssistantAndToolPairsCompactingHistory() {
        // Production scenario observed in chat-memory dump:
        // USER (prev prompt)
        // ASSISTANT (empty) ← stale
        // TOOL (empty responses) ← stale
        // ASSISTANT (real text)
        // TOOL (empty responses) ← stale
        // USER (current prompt)
        // After advisor: USER, ASSISTANT(text), USER. Clean user/assistant alternation, no orphan tool refs.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Schedule a daily message"),
            new AssistantMessage(""),
            ToolResponseMessage.builder()
                .responses(List.of())
                .build(),
            new AssistantMessage("I need a couple more details before building this workflow."),
            ToolResponseMessage.builder()
                .responses(List.of())
                .build(),
            new UserMessage("Use #engineering")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(3);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Schedule a daily message");
        assertThat(patched.get(1)
            .getText()).contains("I need a couple more details");
        assertThat(patched.get(2)
            .getText()).isEqualTo("Use #engineering");
    }

    @Test
    void testWhitespaceOnlyUserMessageIsTreatedAsEmpty() {
        // Anthropic's validator trims whitespace before checking emptiness. A " \n " content block hits
        // the same 400 as a literal empty string — treat both equivalently.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        Prompt prompt = new Prompt(List.of(new UserMessage("  \n\t  ")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(1);
        assertThat(patched.get(0)
            .getText()).isEqualTo("no content");
    }

    @Test
    void testStripsDanglingAssistantToolCallFollowedByUserMessage() {
        // EXACT reproduction of the poisoned-thread failure: the assistant emitted a tool_use, the turn was
        // interrupted before the tool_result persisted, and the user retried. Chat memory replays
        // ASSISTANT(tool_use) directly followed by USER — Anthropic rejects the tool_use with "tool_use ids were
        // found without tool_result blocks immediately after", failing every later turn until the thread is cleared.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage danglingToolUse = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("call-1", "function", "listConnectionsForComponent", "{}")))
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Use OpenAI"),
            danglingToolUse,
            new UserMessage("try again")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Use OpenAI");
        assertThat(patched.get(1)
            .getText()).isEqualTo("try again");
    }

    @Test
    void testKeepsAnsweredToolCallButStripsUnansweredSibling() {
        // Parallel tool calls where only one result reached memory: keep the answered call (its tool_result
        // survives) and drop the unanswered one, so the tool_use/tool_result blocks stay balanced.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(
                new ToolCall("call-1", "function", "answered", "{}"),
                new ToolCall("call-2", "function", "unanswered", "{}")))
            .build();

        ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "answered", "{\"ok\":true}")))
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Q"), assistantMessage, toolResponseMessage, new UserMessage("Next")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(4);

        AssistantMessage patchedAssistant = (AssistantMessage) patched.get(1);

        assertThat(patchedAssistant.getToolCalls()).hasSize(1);
        assertThat(patchedAssistant.getToolCalls()
            .get(0)
            .id()).isEqualTo("call-1");
    }

    @Test
    void testStripsAssistantToolUseOrphanedByEmptyToolResultRemoval() {
        // Composition of the two passes: an all-empty ToolResponseMessage (lossy chat-memory replay) is stripped
        // first, which would orphan the preceding assistant tool_use. The pairing pass then removes that tool_use
        // so no dangling tool_use reaches Anthropic.
        NonEmptyMessagesAdvisor advisor = new NonEmptyMessagesAdvisor();

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new ToolCall("call-1", "function", "searchComponents", "{}")))
            .build();

        Prompt prompt = new Prompt(List.of(
            new UserMessage("Find Slack"),
            assistantMessage,
            ToolResponseMessage.builder()
                .responses(List.of())
                .build(),
            new UserMessage("Anything?")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        List<Message> patched = capturePatchedMessages(advisor, request);

        assertThat(patched).hasSize(2);
        assertThat(patched.get(0)
            .getText()).isEqualTo("Find Slack");
        assertThat(patched.get(1)
            .getText()).isEqualTo("Anything?");
    }

    private static ChatClientResponse dispatch(NonEmptyMessagesAdvisor advisor, ChatClientRequest request) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = mock(ChatClientResponse.class);

        when(chain.nextCall(any())).thenReturn(response);

        return advisor.adviseCall(request, chain);
    }

    private static List<Message> capturePatchedMessages(NonEmptyMessagesAdvisor advisor, ChatClientRequest request) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        advisor.adviseCall(request, chain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(chain).nextCall(captor.capture());

        return captor.getValue()
            .prompt()
            .getInstructions();
    }
}
