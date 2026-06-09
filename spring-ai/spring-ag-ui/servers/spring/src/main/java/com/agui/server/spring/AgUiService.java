package com.agui.server.spring;

import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.core.stream.EventStream;
import com.agui.server.LocalAgent;
import com.agui.server.streamer.AgentStreamer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Spring service for executing agents and streaming events via Server-Sent Events (SSE).
 *
 * @author Pascal Wilbrink
 */
public class AgUiService {

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

        var eventStream = new EventStream<BaseEvent>(
            event -> {
                try {
                    emitter.send(SseEmitter.event().data(" " + objectMapper.writeValueAsString(event)).build());
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            emitter::completeWithError,
            emitter::complete
        );

        this.agentStreamer.streamEvents(agent, parameters, eventStream);

        return emitter;
    }
}
