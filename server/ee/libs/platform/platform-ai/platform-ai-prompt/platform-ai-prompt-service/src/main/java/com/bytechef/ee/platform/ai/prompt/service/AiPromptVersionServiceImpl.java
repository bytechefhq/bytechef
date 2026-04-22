/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.prompt.service;

import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionService;
import com.bytechef.ee.platform.ai.prompt.repository.AiPromptVersionRepository;
import com.bytechef.ee.platform.ai.prompt.util.PromptVariableExtractor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI")
class AiPromptVersionServiceImpl implements AiPromptVersionService {

    private final AiPromptVersionRepository aiPromptVersionRepository;

    AiPromptVersionServiceImpl(AiPromptVersionRepository aiPromptVersionRepository) {
        this.aiPromptVersionRepository = aiPromptVersionRepository;
    }

    @Override
    public AiPromptVersion create(AiPromptVersion promptVersion) {
        Validate.notNull(promptVersion, "promptVersion must not be null");
        Validate.isTrue(promptVersion.getId() == null, "promptVersion id must be null for creation");

        // If the caller didn't provide an explicit variables list, auto-derive from the content's {{name}}
        // placeholders. Keeps the UI truthful when operators paste a template and hit save.
        if (promptVersion.getVariables() == null) {
            promptVersion.setVariables(PromptVariableExtractor.extractAsJson(promptVersion.getContent()));
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
        return aiPromptVersionRepository.findByPromptIdAndActiveAndEnvironment(promptId, true, environment);
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
                () -> new IllegalArgumentException("AiPromptVersion not found with id: " + promptVersionId));

        List<AiPromptVersion> currentlyActiveVersions = aiPromptVersionRepository
            .findAllByPromptIdAndEnvironmentAndActive(targetVersion.getPromptId(), environment, true);

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
