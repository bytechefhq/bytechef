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

package com.bytechef.ee.platform.ai.skill.facade;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AiSkillFacade {

    AiSkill createAiSkill(String name, @Nullable String description, String filename, byte[] bytes);

    AiSkill createAiSkillFromInstructions(
        String name, @Nullable String description, String instructions);

    void deleteAiSkill(long id);

    AiSkill getAiSkill(long id);

    /** Returns the raw bytes of the skill zip archive. */
    byte[] getAiSkillDownload(long id);

    /**
     * Returns the skill metadata and raw zip bytes together, avoiding separate lookups.
     */
    AiSkillDownload getAiSkillWithDownload(long id);

    /**
     * Reads a single file from within the skill zip archive. Path must not contain traversal sequences (..) or be
     * absolute.
     *
     * @throws IllegalArgumentException     if path contains traversal sequences, is absolute, or file is not found
     * @throws java.io.UncheckedIOException if the skill archive is corrupt or unreadable
     */
    String getAiSkillFileContent(long id, String path);

    /**
     * Returns all non-directory entry paths within the skill zip archive, excluding entries with path traversal
     * sequences or absolute paths.
     */
    List<String> getAiSkillFilePaths(long id);

    @SuppressFBWarnings("EI")
    record AiSkillDownload(AiSkill aiSkill, byte[] bytes) {
    }

    List<AiSkill> getAiSkills();

    AiSkill updateAiSkill(long id, String name, @Nullable String description);

    /**
     * Replaces the content of a single file inside the skill zip archive. If {@code path} is {@code null}, defaults to
     * {@code SKILL.md}. The path must not contain traversal sequences (..) or be absolute.
     */
    AiSkill updateAiSkillContent(long id, @Nullable String path, String content);
}
