/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import com.bytechef.ee.automation.ai.gateway.repository.AiPromptVersionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiPromptVersionServiceImpl implements AiPromptVersionService {

    private final AiPromptVersionRepository aiPromptVersionRepository;

    AiPromptVersionServiceImpl(
        AiPromptVersionRepository aiPromptVersionRepository) {

        this.aiPromptVersionRepository = aiPromptVersionRepository;
    }

    @Override
    public AiPromptVersion create(AiPromptVersion promptVersion) {
        Validate.notNull(promptVersion, "promptVersion must not be null");
        Validate.isTrue(promptVersion.getId() == null, "promptVersion id must be null for creation");

        // If the caller didn't provide an explicit variables list, auto-derive from the content's {{name}}
        // placeholders. Keeps the UI truthful when operators paste a template and hit save.
        if (promptVersion.getVariables() == null) {
            promptVersion.setVariables(
                com.bytechef.ee.automation.ai.gateway.util.PromptVariableExtractor
                    .extractAsJson(promptVersion.getContent()));
        }

        return aiPromptVersionRepository.save(promptVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPromptVersion> getVersionsByPrompt(Long promptId) {
        return aiPromptVersionRepository.findAllByPromptIdOrderByVersionNumberDesc(promptId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiPromptVersion> getActiveVersion(Long promptId, String environment) {
        return aiPromptVersionRepository.findByPromptIdAndActiveAndEnvironment(
            promptId, true, environment);
    }

    @Override
    @Transactional(readOnly = true)
    public int getNextVersionNumber(Long promptId) {
        Optional<AiPromptVersion> latestVersion =
            aiPromptVersionRepository.findTopByPromptIdOrderByVersionNumberDesc(promptId);

        return latestVersion.map(version -> version.getVersionNumber() + 1)
            .orElse(1);
    }

    @Override
    public void setActiveVersion(long promptVersionId, String environment) {
        AiPromptVersion targetVersion = aiPromptVersionRepository.findById(promptVersionId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "AiPromptVersion not found with id: " + promptVersionId));

        List<AiPromptVersion> currentlyActiveVersions =
            aiPromptVersionRepository.findAllByPromptIdAndEnvironmentAndActive(
                targetVersion.getPromptId(), environment, true);

        for (AiPromptVersion activeVersion : currentlyActiveVersions) {
            activeVersion.setActive(false);

            aiPromptVersionRepository.save(activeVersion);
        }

        targetVersion.setEnvironment(environment);
        targetVersion.setActive(true);

        aiPromptVersionRepository.save(targetVersion);
    }

    @Override
    public AiPromptVersion update(AiPromptVersion promptVersion) {
        Validate.notNull(promptVersion, "promptVersion must not be null");
        Validate.notNull(promptVersion.getId(), "promptVersion id must not be null for update");

        return aiPromptVersionRepository.save(promptVersion);
    }
}
