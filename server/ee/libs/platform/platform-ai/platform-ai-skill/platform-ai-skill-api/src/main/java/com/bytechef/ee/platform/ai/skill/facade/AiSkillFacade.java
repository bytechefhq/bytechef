/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.facade;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiSkillFacade {

    AiSkill createAiSkill(String name, @Nullable String description, String filename, byte[] bytes);

    AiSkill createAiSkillFromInstructions(
        String name, @Nullable String description, String instructions,
        @Nullable Map<String, String> additionalFiles);

    /**
     * Adds or replaces multiple files inside an existing skill zip archive in one operation. Paths must not contain
     * traversal sequences (..) or be absolute.
     */
    AiSkill createAdditionalFilesInSkill(long id, Map<String, String> additionalFiles);

    default AiSkill createAiSkillFromInstructions(
        String name, @Nullable String description, String instructions) {

        return createAiSkillFromInstructions(name, description, instructions, null);
    }

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
