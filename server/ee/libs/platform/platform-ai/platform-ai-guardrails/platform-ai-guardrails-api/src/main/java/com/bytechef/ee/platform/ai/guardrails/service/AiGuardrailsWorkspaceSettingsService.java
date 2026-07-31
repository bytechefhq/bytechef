/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 */
public interface AiGuardrailsWorkspaceSettingsService {

    Optional<AiGuardrailsWorkspaceSettings> fetchSettings(@Nullable Long workspaceId);

    AiGuardrailsWorkspaceSettings saveSettings(AiGuardrailsWorkspaceSettings settings);
}
