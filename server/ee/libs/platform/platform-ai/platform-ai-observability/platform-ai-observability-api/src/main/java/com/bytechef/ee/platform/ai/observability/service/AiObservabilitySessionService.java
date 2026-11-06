/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;

/**
 * @version ee
 */
public interface AiObservabilitySessionService {

    AiObservabilitySession create(AiObservabilitySession session);

    AiObservabilitySession getSession(long id);
}
