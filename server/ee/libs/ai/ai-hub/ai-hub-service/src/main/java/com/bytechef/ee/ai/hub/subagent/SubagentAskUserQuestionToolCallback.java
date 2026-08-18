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
 * Neither {@link #SPECIALIST_CONTRACT_GUIDANCE} nor {@link #STOP_INSTRUCTION} promises the specialist that its context
 * survives to the follow-up call, because on two of the three surfaces it does not: per-conversation specialist memory
 * is attached only by {@code AiHubConfiguration#wrapDelegate}, while both Copilot panel configurations and all three
 * management-MCP contributors pass an identity {@code chatClientDecorator} — and the MCP surface carries no
 * conversation id to key a session on in the first place. The same tool instance is attached on every surface, so its
 * text has to be true on the weakest one. What both texts do instead is tell the specialist to put what matters into
 * the summary, which travels on every surface.
 * </p>
 *
 * <p>
 * The tool <b>name</b> and <b>input schema</b> are the delegate's verbatim; only the description differs, gaining
 * {@link #SPECIALIST_CONTRACT_GUIDANCE}. That guidance lives here rather than in the specialist's system prompt on
 * purpose. Every specialist prompt file is shared with a Copilot panel agent (see the domain copilot slice pattern),
 * and the panel agent's {@code askUserQuestion} — where it has one at all — follows the OPPOSITE contract: it
 * <i>returns</i> the envelope and keeps going. A shared prompt therefore cannot state this contract truthfully for both
 * readers, and stating it for a panel agent that has no such tool at all would name a tool the agent does not have and
 * kill the turn with "No ToolCallback found". A tool description is read only by the agents actually holding the tool,
 * which is exactly the audience this instruction has.
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

    /**
     * Appended to the delegate's own description. Names the two things a specialist must know that the main agent's
     * reader must not be told: that the question reaches the user through the delegating agent's stream rather than
     * this tool's return value, and that the correct move after asking is to stop.
     */
    private static final String SPECIALIST_CONTRACT_GUIDANCE =
        " You are running as a delegated specialist: this tool poses the question to the user through the agent that" +
            " delegated to you, and returns only an acknowledgement — never the user's answer. Ask at most ONE" +
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
