/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.codeworkflow.file.storage;

import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class CodeWorkflowFileStorageImpl implements CodeWorkflowFileStorage {

    private static final String CODE_WORKFLOWS_DEFINITIONS_DIR = "code_workflows/definitions";
    private static final String CODE_WORKFLOWS_FILES_DIR = "code_workflows/files";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public CodeWorkflowFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteCodeWorkflowDefinition(@NonNull FileEntry definitionFile) {
        fileStorageService.deleteFile(CODE_WORKFLOWS_DEFINITIONS_DIR, definitionFile);
    }

    @Override
    public void deleteCodeWorkflowFile(@NonNull FileEntry codeFile) {
        fileStorageService.deleteFile(CODE_WORKFLOWS_FILES_DIR, codeFile);
    }

    @Override
    public String readCodeWorkflowDefinition(@NonNull FileEntry definitionFile) {
        return CompressionUtils.decompressToString(
            fileStorageService.readFileToBytes(CODE_WORKFLOWS_DEFINITIONS_DIR, definitionFile));
    }

    @Override
    public URL getCodeWorkflowFileURL(@NonNull FileEntry codeFile) {
        return fileStorageService.getFileEntryURL(CODE_WORKFLOWS_FILES_DIR, codeFile);
    }

    @Override
    public FileEntry storeCodeWorkflowDefinition(@NonNull String filename, @NonNull String definition) {
        return fileStorageService.storeFileContent(
            CODE_WORKFLOWS_DEFINITIONS_DIR, filename, CompressionUtils.compress(definition), false);
    }

    @Override
    public FileEntry storeCodeWorkflowFile(@NonNull String filename, @NonNull byte[] bytes) {
        return fileStorageService.storeFileContent(CODE_WORKFLOWS_FILES_DIR, filename, bytes, false);
    }
}
