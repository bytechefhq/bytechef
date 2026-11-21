/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.subagent;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.ai.copilot.tool.context.LookupPropertyTracker;
import com.bytechef.commons.util.JsonUtils;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentAskUserQuestionToolCallback implements ToolCallback {

    private static final String SPECIALIST_CONTRACT_GUIDANCE =
        " You are running as a delegated specialist: this tool poses the question to the user through the agent that" +
            " delegated to you, and returns only an acknowledgement, never the user's answer. Ask at most ONE" +
            " question per delegation, then STOP. Do not guess an answer, and do not keep working past the question." +
            " Return a one-line summary naming what you asked and what you had already established, since the" +
            " delegating agent works from that summary and a later call may reach you with none of this turn's" +
            " reasoning still in hand.";

    private static final String STOP_INSTRUCTION =
        "Question posed to the user. Stop now and return a one-line summary of what you asked and what you had " +
            "already established. A later call will bring the user's answer, but may not bring this turn's " +
            "reasoning with it, so put anything the follow-up needs into that summary.";

    private static final Logger log = LoggerFactory.getLogger(SubagentAskUserQuestionToolCallback.class);

    private final ToolCallback delegate;

    public SubagentAskUserQuestionToolCallback() {
        this.delegate = new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        return ToolDefinition.builder()
            .name(toolDefinition.name())
            .description(toolDefinition.description() + SPECIALIST_CONTRACT_GUIDANCE)
            .inputSchema(toolDefinition.inputSchema())
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String delegateResult = delegate.call(toolInput, withoutLookupPropertyTracker(toolContext));

        if (!isAskEnvelope(delegateResult)) {
            return delegateResult;
        }

        boolean accepted = SubagentAskChannel.offer(delegateResult);

        if (!accepted) {
            return ToolErrors.toolError(
                "A question is already pending for this delegation; ask one question, stop, and continue after the " +
                    "answer.");
        }

        return STOP_INSTRUCTION;
    }

    private static @Nullable ToolContext withoutLookupPropertyTracker(@Nullable ToolContext toolContext) {
        if (toolContext == null ||
            !toolContext.getContext()
                .containsKey(LookupPropertyTracker.TOOL_CONTEXT_LOOKUP_PROPERTY_TRACKER_KEY)) {

            return toolContext;
        }

        Map<String, Object> context = new HashMap<>(toolContext.getContext());

        context.remove(LookupPropertyTracker.TOOL_CONTEXT_LOOKUP_PROPERTY_TRACKER_KEY);

        return new ToolContext(context);
    }

    private static boolean isAskEnvelope(String delegateResult) {
        JsonNode rootJsonNode;

        try {
            rootJsonNode = JsonUtils.readTree(delegateResult);
        } catch (JacksonException exception) {
            log.warn("askUserQuestion delegate returned non-JSON; treating as a failure and asking nothing", exception);

            return false;
        }

        JsonNode kindJsonNode = rootJsonNode.get("kind");

        return kindJsonNode != null && AskUserQuestionToolCallback.KIND.equals(kindJsonNode.asString());
    }
}
