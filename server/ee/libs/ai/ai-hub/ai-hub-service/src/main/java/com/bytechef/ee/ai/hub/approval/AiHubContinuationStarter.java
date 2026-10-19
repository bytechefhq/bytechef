/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import java.util.Optional;

/**
 * Starts the continuation turn that reports a resolved approval back into its chat. The approval facade calls it after
 * the decision is committed and the tool has run, inside the requester's security context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface AiHubContinuationStarter {

    /**
     * Prefix of every continuation message. The client renders a user message starting with it as a status line on the
     * approval card rather than a chat bubble.
     */
    String CONTINUATION_PREFIX = "[tool-approval #";

    /**
     * Posts {@code message} as a user message on {@code chat}'s thread and runs the agent of the approval's mode.
     * {@code approval} supplies the mode, environment, LLM selection and requester; {@code workspaceId} is the chat's
     * workspace. Returns the started run's id, or empty when no continuation started now: no agent is registered for
     * the mode, or a run is in flight for the thread, in which case the continuation starts once that run ends.
     */
    Optional<String> startContinuation(AiHubToolApproval approval, AiHubChat chat, long workspaceId, String message);
}
