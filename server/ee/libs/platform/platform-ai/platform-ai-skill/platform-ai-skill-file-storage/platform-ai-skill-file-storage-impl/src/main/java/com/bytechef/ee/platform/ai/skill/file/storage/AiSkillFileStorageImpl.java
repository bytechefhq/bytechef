/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiSkillFileStorageImpl implements AiSkillFileStorage {

    private static final String AI_AGENT_SKILLS_FILES_DIR = "ai_agent_skills";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public AiSkillFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteAiSkillFile(FileEntry skillFile) {
        fileStorageService.deleteFile(AI_AGENT_SKILLS_FILES_DIR, skillFile);
    }

    @Override
    public byte[] readAiSkillFileBytes(FileEntry skillFile) {
        return fileStorageService.readFileToBytes(AI_AGENT_SKILLS_FILES_DIR, skillFile);
    }

    @Override
    public FileEntry storeAiSkillFile(String filename, byte[] bytes) {
        return fileStorageService.storeFileContent(AI_AGENT_SKILLS_FILES_DIR, filename, bytes);
    }
}
