/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityTraceService {

    AiObservabilityTrace create(AiObservabilityTrace trace);

    void deleteOlderThan(Instant date);

    AiObservabilityTrace getTrace(long id);

    List<AiObservabilityTrace> getTracesBySession(Long sessionId);

    void update(AiObservabilityTrace trace);

    AiObservabilityTrace setTraceTags(long traceId, List<Long> tagIds);
}
