/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry.InFlightRun;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

/**
 * AG-UI dispatch for AI Hub that survives client disconnects. Replaces the role of
 * {@link com.agui.server.spring.AgUiService AgUiService} for the {@code ai_hub} controller flow specifically because we
 * need behaviours the upstream service can't offer:
 *
 * <ul>
 * <li>The agent's reactive stream is decoupled from any one HTTP {@link SseEmitter}. A refresh kills the emitter; the
 * run keeps producing events into its replay sink, and a later attach call resumes from where the previous emitter left
 * off.</li>
 * <li>A second POST for the same {@code threadId} doesn't start a duplicate agent run. {@link #runAgent} sees the
 * registered in-flight run and returns an emitter attached to its sink — the original run keeps streaming, the second
 * caller just gets the same events.</li>
 * <li>A separate {@link #attachToRun} call exposes the same sink under a different HTTP verb so the client's mount-time
 * probe can latch onto a streaming task without re-submitting the user's message.</li>
 * </ul>
 *
 * <p>
 * Single-instance scope matches {@link InFlightAiHubRunRegistry} — see that class for the Phase-2 follow-up notes on
 * distributing across replicas.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
public class AiHubChatStreamer {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatStreamer.class);

    private final InFlightAiHubRunRegistry registry;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatStreamer(InFlightAiHubRunRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    /**
     * Starts (or re-attaches to) an agent run for the given parameters, returning an {@link SseEmitter} that streams
     * the run's events. {@code threadId} is the server-verified id from the controller's ownership gate — not the raw
     * client-supplied value — so that registry keying matches the same identity used for the attach endpoint.
     *
     * <p>
     * <b>Behaviour matrix</b>:
     * </p>
     * <ul>
     * <li>No run in flight → start a new agent run; the emitter receives events from sink subscription.</li>
     * <li>Run already in flight for this {@code threadId} → return an emitter attached to the existing sink. The agent
     * is NOT re-invoked. Useful when assistant-ui's runtime retriggers POST on mount (rare, but happens with some
     * adapter shapes).</li>
     * <li>{@code threadId} is null → fall back to legacy single-shot behaviour (no registry, no attach support). This
     * is the very-first-turn path before a task row exists; subsequent turns always carry the thread id.</li>
     * </ul>
     */
    public SseEmitter runAgent(LocalAgent agent, AgUiParameters agUiParameters, String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return runWithoutRegistry(agent, agUiParameters);
        }

        InFlightRun run = registry.startRun(threadId, agUiParameters.getRunId());

        SseEmitter emitter = buildEmitterForRun(run);

        // startRun returns an already-terminated run when a Stop click tombstoned this runId before the POST
        // arrived. The sink is already complete, so the emitter just delivers the terminal signal; invoking the
        // agent would burn an LLM call on a turn the user already cancelled.
        if (run.isTerminated()) {
            return emitter;
        }

        // The registry's startRun returns the existing run when one is already streaming; in that case we MUST NOT
        // re-invoke the agent — the existing run is already pumping events into the sink. The emitter we just
        // built is subscribed to the same sink, so this caller will see those events identically to the original.
        if (!run.getRunId()
            .equals(agUiParameters.getRunId())) {

            log.debug(
                "POST for threadId={} arrived during an in-flight run (existing runId={}, requested runId={}); attaching",
                threadId, run.getRunId(), agUiParameters.getRunId());

            return emitter;
        }

        startAgentAsync(agent, agUiParameters, threadId);

        return emitter;
    }

    /**
     * Returns an emitter attached to an existing run's event sink. Empty when no run is registered for {@code threadId}
     * — the controller surfaces that as 404 so the client knows to fall back to history-only rendering.
     */
    public Optional<SseEmitter> attachToRun(String threadId) {
        Optional<Flux<BaseEvent>> fluxOptional = registry.attach(threadId);

        return fluxOptional.map(this::buildEmitterForFlux);
    }

    /**
     * Single-shot fall-through used when the controller couldn't resolve a verified thread id (e.g. very first turn,
     * before the task row exists). Mirrors the original {@code AgUiService.runAgent} behaviour: emitter is the sole
     * subscriber; a disconnect kills the agent's emission target but the run itself keeps going as a side-effect leak.
     * Acceptable because every subsequent turn carries the thread id and goes through the registered path.
     */
    private SseEmitter runWithoutRegistry(LocalAgent agent, AgUiParameters agUiParameters) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        RunAgentParameters parameters = buildParameters(agUiParameters);

        agent.runAgent(parameters, new AgentSubscriber() {

            @Override
            public void onEvent(BaseEvent event) {
                writeEvent(emitter, event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                emitter.complete();
            }

            @Override
            public void onRunFailed(AgentSubscriberParams params, Throwable throwable) {
                emitter.completeWithError(throwable);
            }
        });

        return emitter;
    }

    private void startAgentAsync(LocalAgent agent, AgUiParameters agUiParameters, String threadId) {
        RunAgentParameters parameters = buildParameters(agUiParameters);

        agent.runAgent(parameters, new AgentSubscriber() {

            @Override
            public void onEvent(BaseEvent event) {
                registry.onEvent(threadId, event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                registry.complete(threadId);
            }

            @Override
            public void onRunFailed(AgentSubscriberParams params, Throwable throwable) {
                log.warn("Agent run failed for threadId={}", threadId, throwable);

                registry.fail(threadId, throwable);
            }
        });
    }

    private SseEmitter buildEmitterForRun(InFlightRun run) {
        return buildEmitterForFlux(run.asFlux());
    }

    private SseEmitter buildEmitterForFlux(Flux<BaseEvent> flux) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        Disposable subscription = flux.subscribe(
            event -> writeEvent(emitter, event),
            error -> emitter.completeWithError(error),
            emitter::complete);

        // Client disconnect / timeout / completion all need to tear down the per-emitter subscription so we stop
        // trying to push to a dead emitter. The registry is unaffected — other subscribers continue receiving
        // events, and the agent run keeps going.
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(throwable -> subscription.dispose());

        return emitter;
    }

    private void writeEvent(SseEmitter emitter, BaseEvent event) {
        try {
            emitter.send(SseEmitter.event()
                .data(" " + objectMapper.writeValueAsString(event))
                .build());
        } catch (IOException exception) {
            // Client disconnect is the common cause. Tear down THIS emitter's subscription via completeWithError;
            // the registry's sink is unaffected so other subscribers (and a future attach) still get events.
            emitter.completeWithError(exception);
        }
    }

    private static RunAgentParameters buildParameters(AgUiParameters agUiParameters) {
        return RunAgentParameters.builder()
            .threadId(agUiParameters.getThreadId())
            .runId(agUiParameters.getRunId())
            .messages(agUiParameters.getMessages())
            .tools(agUiParameters.getTools())
            .context(agUiParameters.getContext())
            .forwardedProps(agUiParameters.getForwardedProps())
            .state(agUiParameters.getState())
            .build();
    }
}
