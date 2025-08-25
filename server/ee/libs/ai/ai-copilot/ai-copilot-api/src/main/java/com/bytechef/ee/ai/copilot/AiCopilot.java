/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot;

import com.bytechef.ee.ai.copilot.dto.ContextDTO;
import java.util.Map;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiCopilot {

    Flux<Map<String, ?>> chat(String message, ContextDTO contextDTO, String conversationId);
}
