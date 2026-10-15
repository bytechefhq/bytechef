package com.agui.server.spring;

import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.core.stream.EventStream;
import com.agui.server.EventFactory;
import com.agui.server.LocalAgent;
import com.agui.server.streamer.AgentStreamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring service for executing agents and streaming events via Server-Sent Events (SSE).
 *
 * @author Pascal Wilbrink
 */
public class AgUiService {

    private static final Logger log = LoggerFactory.getLogger(AgUiService.class);

    private final AgentStreamer agentStreamer;

    private final ObjectMapper objectMapper;

    public AgUiService(
        final AgentStreamer agentStreamer,
        final ObjectMapper objectMapper
    ) {
        this.agentStreamer = agentStreamer;
        this.objectMapper = objectMapper;
    }

    public SseEmitter runAgent(final LocalAgent agent, final AgUiParameters agUiParameters) {
        var parameters = RunAgentParameters.builder()
            .threadId(agUiParameters.getThreadId())
            .runId(agUiParameters.getRunId())
            .messages(agUiParameters.getMessages())
            .tools(agUiParameters.getTools())
            .context(agUiParameters.getContext())
            .forwardedProps(agUiParameters.getForwardedProps())
            .state(agUiParameters.getState())
            .build();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        AtomicBoolean clientDisconnected = new AtomicBoolean(false);

        var eventStream = new EventStream<BaseEvent>(
            event -> {
                if (clientDisconnected.get()) {
                    return;
                }

                try {
                    emitter.send(SseEmitter.event().data(" " + objectMapper.writeValueAsString(event)).build());
                } catch (IOException | IllegalStateException e) {
                    onClientDisconnected(agent, agUiParameters, emitter, clientDisconnected, e);
                }
            },
            error -> {
                if (clientDisconnected.get()) {
                    return;
                }

                log.error("Agent '{}' run failed for threadId={} runId={}",
                    agent.getAgentId(), agUiParameters.getThreadId(), agUiParameters.getRunId(), error);

                try {
                    emitter.send(SseEmitter.event()
                        .data(" " + objectMapper.writeValueAsString(
                            EventFactory.runErrorEvent("The agent run failed. Reference: " + agUiParameters.getRunId())))
                        .build());

                    emitter.complete();
                } catch (IOException | IllegalStateException e) {
                    onClientDisconnected(agent, agUiParameters, emitter, clientDisconnected, e);
                }
            },
            () -> {
                if (!clientDisconnected.get()) {
                    emitter.complete();
                }
            }
        );

        this.agentStreamer.streamEvents(agent, parameters, eventStream);

        return emitter;
    }

    private static void onClientDisconnected(
        LocalAgent agent, AgUiParameters agUiParameters, SseEmitter emitter, AtomicBoolean clientDisconnected,
        Exception exception) {

        if (!clientDisconnected.compareAndSet(false, true)) {
            return;
        }

        log.debug("Agent '{}' client disconnected for threadId={} runId={}",
            agent.getAgentId(), agUiParameters.getThreadId(), agUiParameters.getRunId(), exception);

        try {
            emitter.completeWithError(exception);
        } catch (IllegalStateException illegalStateException) {
            log.trace("Emitter already completed for runId={}", agUiParameters.getRunId(), illegalStateException);
        }
    }
}
