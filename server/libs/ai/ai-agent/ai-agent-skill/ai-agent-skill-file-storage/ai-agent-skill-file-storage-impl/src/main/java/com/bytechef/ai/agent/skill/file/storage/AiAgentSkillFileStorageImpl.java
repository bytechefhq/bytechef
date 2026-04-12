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

package com.bytechef.ai.agent.skill.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class AiAgentSkillFileStorageImpl implements AiAgentSkillFileStorage {

    private static final String AI_AGENT_SKILLS_FILES_DIR = "ai_agent_skills";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public AiAgentSkillFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteAiAgentSkillFile(FileEntry skillFile) {
        fileStorageService.deleteFile(AI_AGENT_SKILLS_FILES_DIR, skillFile);
    }

    @Override
    public byte[] readAiAgentSkillFileBytes(FileEntry skillFile) {
        return fileStorageService.readFileToBytes(AI_AGENT_SKILLS_FILES_DIR, skillFile);
    }

    @Override
    public FileEntry storeAiAgentSkillFile(String filename, byte[] bytes) {
        return fileStorageService.storeFileContent(AI_AGENT_SKILLS_FILES_DIR, filename, bytes);
    }
}
