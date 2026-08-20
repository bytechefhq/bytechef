/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest.facade;

import com.agui.server.spring.AgUiParameters;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Facade for the copilot chat run. Hosts the workflow-scope authorization guard on the client-supplied
 * {@code workflowId} carried in the run state, so it applies to every caller of the facade rather than only the REST
 * entry point.
 *
 * <p>
 * The whole run moved here rather than only the guard: the scope required depends on the run's {@code mode} (a BUILD
 * turn mutates the workflow and needs {@code WORKFLOW_EDIT}, every other turn only reads and needs
 * {@code WORKFLOW_VIEW}), so the check cannot be split from the state the agent is dispatched with without reading that
 * state twice and risking the two reads drifting apart.
 *
 * <p>
 * The facade returns {@link SseEmitter}, a Spring MVC type, because the run <em>is</em> an SSE stream — the AG-UI
 * service produces the emitter and there is no non-web representation of it to hand back. It therefore lives in the
 * same module as the controller it serves.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CopilotChatFacade {

    SseEmitter chat(String agentId, AgUiParameters agUiParameters);
}
