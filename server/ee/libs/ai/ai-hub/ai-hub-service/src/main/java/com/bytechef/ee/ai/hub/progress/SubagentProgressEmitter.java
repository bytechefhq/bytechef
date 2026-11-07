/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.CustomEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentProgressEmitter {

    public static final String EVENT_NAME = "subagent-progress";
    public static final String TOOL_CONTEXT_KEY = "aiHubSubagentProgressEmitter";

    private static final Logger log = LoggerFactory.getLogger(SubagentProgressEmitter.class);

    private final Object lock = new Object();
    private final AgentSubscriber subscriber;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SubagentProgressEmitter(AgentSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    public static @Nullable SubagentProgressEmitter fetch(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        return context.get(TOOL_CONTEXT_KEY) instanceof SubagentProgressEmitter emitter ? emitter : null;
    }

    void emit(@Nullable String toolCallId, String subagentName, Phase phase, String text) {
        Map<String, Object> value = new LinkedHashMap<>();

        if (toolCallId != null) {
            value.put("toolCallId", toolCallId);
        }

        value.put("subagentName", subagentName);
        value.put("phase", phase.wireValue());
        value.put("text", text);

        CustomEvent event = new CustomEvent(EVENT_NAME, value);

        synchronized (lock) {
            try {
                subscriber.onEvent(event);
                subscriber.onCustomEvent(event);
            } catch (RuntimeException exception) {
                log.warn("Could not deliver the {} progress event of {}", phase.wireValue(), subagentName, exception);
            }
        }
    }

    @Override
    public String toString() {
        return "SubagentProgressEmitter";
    }

    public enum Phase {

        STARTED, STEP, DONE, FAILED;

        public String wireValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
