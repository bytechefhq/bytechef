package com.agui.spring.ai;

import com.agui.core.event.BaseEvent;
import com.agui.core.state.State;
import com.agui.server.EventFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

public class StateTool {

    private final SpringAIAgent agent;
    private final List<BaseEvent> deferredEvents;
    private final JsonHelper jsonHelper;

    public StateTool(
        final SpringAIAgent agent,
        final List<BaseEvent> deferredEvents
    ) {
        this.agent = agent;
        this.deferredEvents = deferredEvents;
        this.jsonHelper = new JsonHelper();
    }

    @Tool(description = "Update the current state", returnDirect = true)
    void updateState(@ToolParam(description = "The new state as a json string. Use the same keys as the existing state") final String stateJson) {
        var stateMap = jsonHelper.fromJson(stateJson, new ParameterizedTypeReference<Map<String, Object>>(){});

        var state = new State(stateMap);

        agent.setState(state);

        this.deferredEvents.add(EventFactory.stateSnapshotEvent(state));
    }
}

