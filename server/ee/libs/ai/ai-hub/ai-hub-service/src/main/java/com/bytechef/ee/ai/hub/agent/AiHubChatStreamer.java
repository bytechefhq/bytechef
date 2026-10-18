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
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
public class AiHubChatStreamer {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatStreamer.class);

    private final InFlightAiHubRunRegistry registry;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatStreamer(InFlightAiHubRunRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public SseEmitter stream(LocalAgent agent, AgUiParameters agUiParameters, InFlightRun run) {
        SseEmitter emitter = buildEmitter(run.asFlux());

        start(agent, agUiParameters, run);

        return emitter;
    }

    public void start(LocalAgent agent, AgUiParameters agUiParameters, InFlightRun run) {
        if (run.isTerminated()) {
            return;
        }

        agent.runAgent(buildParameters(agUiParameters, run), new AgentSubscriber() {

            @Override
            public void onEvent(BaseEvent event) {
                run.emit(event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                run.complete();
            }

            @Override
            public void onRunFailed(AgentSubscriberParams params, Throwable throwable) {
                log.warn("Agent run failed for threadId={}", run.getThreadId(), throwable);

                run.fail(throwable);
            }
        });
    }

    public Optional<SseEmitter> attach(String threadId) {
        Optional<Flux<BaseEvent>> fluxOptional = registry.attach(threadId);

        return fluxOptional.map(this::buildEmitter);
    }

    private SseEmitter buildEmitter(Flux<BaseEvent> flux) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        Disposable subscription = flux.subscribe(
            event -> writeEvent(emitter, event), emitter::completeWithError, emitter::complete);

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
            emitter.completeWithError(exception);
        }
    }

    private static RunAgentParameters buildParameters(AgUiParameters agUiParameters, InFlightRun run) {
        return RunAgentParameters.builder()
            .threadId(run.getThreadId())
            .runId(agUiParameters.getRunId())
            .messages(agUiParameters.getMessages())
            .tools(List.of())
            .context(agUiParameters.getContext())
            .forwardedProps(agUiParameters.getForwardedProps())
            .state(agUiParameters.getState())
            .build();
    }
}
