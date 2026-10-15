package com.agui.server.spring;

import com.agui.core.agent.Agent;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.core.state.State;
import com.agui.core.stream.EventStream;
import com.agui.server.EventFactory;
import com.agui.server.LocalAgent;
import com.agui.server.streamer.AgentStreamer;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgUiServiceTest {

    @Test
    void shouldSendAGenericRunErrorAndCompleteWhenTheRunFails() throws Exception {
        var capturedEventStream = new AtomicReference<EventStream<BaseEvent>>();

        var emitter = newService(capturedEventStream).runAgent(newAgent(), newParameters());

        capturedEventStream.get().error(new IllegalStateException("password authentication failed for user \"bytechef\""));

        String sentData = String.join("", getSentData(emitter));

        assertThat(sentData)
            .contains("RUN_ERROR")
            .contains("The agent run failed. Reference: run-1")
            .doesNotContain("password");
        assertThat(isComplete(emitter)).isTrue();
    }

    @Test
    void shouldStopSendingQuietlyOnceTheClientHasDisconnected() throws Exception {
        var capturedEventStream = new AtomicReference<EventStream<BaseEvent>>();

        var emitter = newService(capturedEventStream).runAgent(newAgent(), newParameters());

        emitter.complete();

        EventStream<BaseEvent> eventStream = capturedEventStream.get();

        assertThatCode(() -> eventStream.next(EventFactory.runStartedEvent("thread-1", "run-1")))
            .doesNotThrowAnyException();
        assertThatCode(() -> eventStream.error(new IllegalStateException("boom")))
            .doesNotThrowAnyException();
        assertThatCode(eventStream::complete)
            .doesNotThrowAnyException();

        assertThat(getSentData(emitter)).isEmpty();
    }

    private static AgUiService newService(AtomicReference<EventStream<BaseEvent>> capturedEventStream) {
        AgentStreamer agentStreamer = new AgentStreamer() {
            @Override
            public void streamEvents(Agent agent, RunAgentParameters parameters, EventStream<BaseEvent> eventStream) {
                capturedEventStream.set(eventStream);
            }
        };

        return new AgUiService(agentStreamer, new JsonMapper());
    }

    private static LocalAgent newAgent() {
        LocalAgent agent = mock(LocalAgent.class);

        when(agent.getAgentId()).thenReturn("test");

        return agent;
    }

    private static AgUiParameters newParameters() {
        var parameters = new AgUiParameters();

        parameters.setThreadId("thread-1");
        parameters.setRunId("run-1");
        parameters.setMessages(new ArrayList<>());
        parameters.setState(new State());

        return parameters;
    }

    private static List<String> getSentData(SseEmitter emitter) throws ReflectiveOperationException {
        Field earlySendAttemptsField = emitter.getClass()
            .getSuperclass()
            .getDeclaredField("earlySendAttempts");

        earlySendAttemptsField.setAccessible(true);

        List<String> sentData = new ArrayList<>();

        for (Object dataWithMediaType : (Collection<?>) earlySendAttemptsField.get(emitter)) {
            Object data = dataWithMediaType.getClass()
                .getMethod("getData")
                .invoke(dataWithMediaType);

            sentData.add(String.valueOf(data));
        }

        return sentData;
    }

    private static boolean isComplete(SseEmitter emitter) throws ReflectiveOperationException {
        Field completeField = emitter.getClass()
            .getSuperclass()
            .getDeclaredField("complete");

        completeField.setAccessible(true);

        return (boolean) completeField.get(emitter);
    }
}
