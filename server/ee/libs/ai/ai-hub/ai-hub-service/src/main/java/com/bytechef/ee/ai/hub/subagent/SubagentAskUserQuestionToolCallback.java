/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.commons.util.JsonUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * The {@code askUserQuestion} tool as a <b>specialist subagent</b> sees it: it poses a real multiple-choice question to
 * the user instead of handing the question back to the parent agent as prose to paraphrase.
 *
 * <p>
 * Decorates the main agent's {@link AskUserQuestionToolCallback} rather than rebuilding it. That callback already owns
 * the strict input validation, the question capture, and the exact envelope the chat client renders; a second
 * implementation here would have to duplicate all of it and would drift from the renderer the first time either side
 * changed a field. Decorating makes the payload byte-identical by construction rather than by convention.
 * </p>
 *
 * <p>
 * The one behavioural difference is what goes back to the LLM. The main agent <i>returns</i> the envelope, because the
 * client renders the main agent's tool result directly. A specialist's tool result is never rendered — it is consumed
 * by the specialist's own LLM — so this one writes the envelope to {@link SubagentAskChannel} (whence the delegate
 * {@code ToolCallback} returns it as its own result, putting it on the parent's stream) and returns a
 * {@link #STOP_INSTRUCTION}. Returning the envelope here would be a real bug rather than a style choice: the specialist
 * would read its own question back as though it were the answer and invent the user's decision — the exact failure this
 * tool exists to prevent.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentAskUserQuestionToolCallback implements ToolCallback {

    private static final String STOP_INSTRUCTION =
        "Question posed to the user. Stop now and return a one-line summary of what you asked. " +
            "You will be re-invoked with the user's answer and your prior context intact.";

    private static final Logger log = LoggerFactory.getLogger(SubagentAskUserQuestionToolCallback.class);

    private final ToolCallback delegate;

    public SubagentAskUserQuestionToolCallback() {
        this.delegate = new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String delegateResult = delegate.call(toolInput, toolContext);

        if (!isAskEnvelope(delegateResult)) {
            // A validation failure or runtime error from the delegate — pass its own message through so the
            // specialist's LLM can correct the input, and leave the channel empty.
            return delegateResult;
        }

        boolean accepted = SubagentAskChannel.offer(delegateResult);

        if (!accepted) {
            return ToolErrors.toolError(
                "A question is already pending for this delegation — ask one question, stop, and continue after the " +
                    "answer.");
        }

        return STOP_INSTRUCTION;
    }

    /**
     * Distinguishes the rendered question envelope from the delegate's tool-error JSON. Keys off the shared
     * {@link AskUserQuestionToolCallback#KIND} constant so the two cannot drift apart.
     */
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
