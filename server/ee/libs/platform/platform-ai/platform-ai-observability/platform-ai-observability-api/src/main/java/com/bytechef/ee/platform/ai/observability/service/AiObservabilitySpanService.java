/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.dto.AiPromptVersionMetrics;
import java.util.List;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiObservabilitySpanService {

    void create(AiObservabilitySpan span);

    AiPromptVersionMetrics getPromptVersionMetrics(long promptVersionId);

    AiObservabilitySpan getSpan(long id);

    List<AiObservabilitySpan> getSpansByTrace(Long traceId);

    void update(AiObservabilitySpan span);
}
