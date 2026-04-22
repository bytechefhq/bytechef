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

package com.bytechef.ee.platform.ai.prompt.repository;

import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @author Ivica Cardic
 */
public interface AiPromptVersionRepository extends ListCrudRepository<AiPromptVersion, Long> {

    List<AiPromptVersion> findAllByPromptId(Long promptId);

    List<AiPromptVersion> findAllByPromptIdOrderByVersionNumberDesc(Long promptId);

    Optional<AiPromptVersion> findByPromptIdAndActiveAndEnvironment(
        Long promptId, boolean active, String environment);

    Optional<AiPromptVersion> findTopByPromptIdOrderByVersionNumberDesc(Long promptId);

    List<AiPromptVersion> findAllByPromptIdAndEnvironmentAndActive(
        Long promptId, String environment, boolean active);
}
