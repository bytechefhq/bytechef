/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilitySpanService {

    void create(AiObservabilitySpan span);

    List<AiObservabilitySpan> getSpansByTrace(Long traceId);

    void update(AiObservabilitySpan span);
}
