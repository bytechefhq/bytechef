/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiPromptVersionService {

    AiPromptVersion create(AiPromptVersion promptVersion);

    List<AiPromptVersion> getVersionsByPrompt(Long promptId);

    Optional<AiPromptVersion> getActiveVersion(Long promptId, String environment);

    int getNextVersionNumber(Long promptId);

    void setActiveVersion(long promptVersionId, String environment);

    AiPromptVersion update(AiPromptVersion promptVersion);
}
