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
 * @version ee
 * @author Ivica Cardic
 */
public interface CopilotChatFacade {

    SseEmitter chat(String agentId, AgUiParameters agUiParameters);
}
