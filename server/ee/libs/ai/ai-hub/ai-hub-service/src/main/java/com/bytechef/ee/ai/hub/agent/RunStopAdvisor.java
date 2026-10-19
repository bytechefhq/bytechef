/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

/**
 * Stops a hub run when the user stops it. Ordered first, it ends the run's model stream as soon as
 * {@link InFlightAiHubRunRegistry#cancel} is called for the run's thread, which cancels everything inside it: the model
 * call, the tool-calling loop and the session memory writes of the unfinished round. The agent then finishes and the
 * registry releases the thread.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class RunStopAdvisor implements CallAdvisor, StreamAdvisor {

    private final InFlightAiHubRunRegistry inFlightRunRegistry;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RunStopAdvisor(InFlightAiHubRunRegistry inFlightRunRegistry) {
        this.inFlightRunRegistry = inFlightRunRegistry;
    }

    @Override
    public String getName() {
        return "ai-hub-run-stop";
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        if (!(chatClientRequest.context()
            .get(ChatMemory.CONVERSATION_ID) instanceof String threadId)) {

            return streamAdvisorChain.nextStream(chatClientRequest);
        }

        return streamAdvisorChain.nextStream(chatClientRequest)
            .takeUntilOther(inFlightRunRegistry.stopRequested(threadId));
    }
}
