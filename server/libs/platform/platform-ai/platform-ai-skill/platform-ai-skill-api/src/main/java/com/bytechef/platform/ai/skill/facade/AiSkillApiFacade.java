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

package com.bytechef.platform.ai.skill.facade;

import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade.AiSkillDownload;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * User-facing facade for the AI skill HTTP entry points (GraphQL controller and the download REST controller). It wraps
 * {@link AiSkillFacade} and enforces per-user owner-isolation on every by-id operation.
 *
 * <p>
 * AI skills are per-user resources ({@code AiSkill.createdBy} is the owner's login). The shared {@link AiSkillFacade} /
 * {@code AiSkillService} are also invoked by the runtime AI agent tools (which run without a platform-user security
 * context), so the authorization cannot live on the shared service without breaking agent execution; it lives here, on
 * the surface that always carries an authenticated user. The runtime keeps calling {@link AiSkillFacade} directly.
 *
 * @author Ivica Cardic
 */
public interface AiSkillApiFacade {

    AiSkill createAiSkill(String name, @Nullable String description, String filename, byte[] bytes);

    AiSkill createAiSkillFromInstructions(String name, @Nullable String description, String instructions);

    AiSkill generateAiSkill(String prompt, int environmentId);

    void deleteAiSkill(long id);

    AiSkill getAiSkill(long id);

    AiSkillDownload getAiSkillWithDownload(long id);

    String getAiSkillFileContent(long id, String path);

    List<String> getAiSkillFilePaths(long id);

    List<AiSkill> getAiSkills();

    AiSkill updateAiSkill(long id, String name, @Nullable String description);

    AiSkill updateAiSkillContent(long id, @Nullable String path, String content);
}
