/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.service;

import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the workspace system prompt with a single {@link Property} row per workspace, keyed by
 * {@link WorkspaceSystemPrompt#PROPERTY_KEY} under {@link Property.Scope#WORKSPACE} — mirroring
 * {@code AiGuardrailsWorkspaceSettingsServiceImpl}'s storage shape. No PLATFORM-scope tenant-default row exists for
 * this feature.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
class WorkspaceSystemPromptServiceImpl implements WorkspaceSystemPromptService {

    private static final String KEY_PROMPT = "prompt";

    private final PropertyService propertyService;

    WorkspaceSystemPromptServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> fetchWorkspaceSystemPrompt(long workspaceId) {
        return propertyService
            .fetchProperty(WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, workspaceId)
            .map(property -> (String) property.getValue()
                .get(KEY_PROMPT))
            .filter(prompt -> prompt != null && !prompt.isBlank());
    }

    @Override
    public Optional<String> saveWorkspaceSystemPrompt(long workspaceId, @Nullable String prompt) {
        String stripped = prompt == null ? "" : prompt.strip();

        if (stripped.isEmpty()) {
            propertyService.delete(WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, workspaceId);

            return Optional.empty();
        }

        Validate.isTrue(
            stripped.length() <= WorkspaceSystemPrompt.MAX_LENGTH,
            "prompt must be at most %d characters", WorkspaceSystemPrompt.MAX_LENGTH);

        propertyService.save(
            WorkspaceSystemPrompt.PROPERTY_KEY, Map.of(KEY_PROMPT, stripped), Property.Scope.WORKSPACE, workspaceId);

        return Optional.of(stripped);
    }
}
