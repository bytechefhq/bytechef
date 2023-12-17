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

package com.bytechef.hermes.file.storage;

import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class TriggerFileStorageImpl implements TriggerFileStorage {

    private static final String TRIGGER_EXECUTION_FILES_DIR = "workflow_outputs_trigger_executions";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public TriggerFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object readTriggerExecutionOutput(@NonNull FileEntry fileEntry) {
        Validate.notNull(fileEntry, "'fileEntry' must not be null");

        return JsonUtils.read(
            CompressionUtils.decompressToString(
                fileStorageService.readFileToBytes(TRIGGER_EXECUTION_FILES_DIR, fileEntry)),
            Object.class);
    }

    @Override
    public FileEntry storeTriggerExecutionOutput(long triggerExecutionId, @NonNull Object output) {
        Validate.notNull(output, "'output' must not be null");

        return fileStorageService.storeFileContent(
            TRIGGER_EXECUTION_FILES_DIR, triggerExecutionId + ".json",
            CompressionUtils.compress(JsonUtils.write(output)));
    }
}
