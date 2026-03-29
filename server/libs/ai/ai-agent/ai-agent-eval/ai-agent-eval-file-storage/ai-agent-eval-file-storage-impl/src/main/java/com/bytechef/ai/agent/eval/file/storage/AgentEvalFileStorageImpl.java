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

package com.bytechef.ai.agent.eval.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class AgentEvalFileStorageImpl implements AgentEvalFileStorage {

    private static final String AGENT_EVAL_TRANSCRIPTS_DIR = "agent_eval_transcripts";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public AgentEvalFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteTranscriptFile(FileEntry transcriptFile) {
        fileStorageService.deleteFile(AGENT_EVAL_TRANSCRIPTS_DIR, transcriptFile);
    }

    @Override
    public byte[] readTranscriptFile(FileEntry transcriptFile) {
        return fileStorageService.readFileToBytes(AGENT_EVAL_TRANSCRIPTS_DIR, transcriptFile);
    }

    @Override
    public FileEntry storeTranscriptFile(String filename, byte[] content) {
        return fileStorageService.storeFileContent(AGENT_EVAL_TRANSCRIPTS_DIR, filename, content);
    }
}
