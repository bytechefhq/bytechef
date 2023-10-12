
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

package com.bytechef.atlas.file.storage.facade;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TaskFileStorageFacadeImpl implements TaskFileStorageFacade {

    private static final String CONTEXT_FILES_DIR = "workflow_outputs_contexts";
    private static final String JOB_FILES_DIR = "workflow_outputs_jobs";
    private static final String TASK_EXECUTION_FILES_DIR = "workflow_outputs_task_executions";

    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public TaskFileStorageFacadeImpl(FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, ?> readContextValue(@NonNull FileEntry fileEntry) {
        return JsonUtils.read(
            CompressionUtils.decompressToString(fileStorageService.readFileToBytes(CONTEXT_FILES_DIR, fileEntry)),
            new TypeReference<>() {}, objectMapper);
    }

    @Override
    public Map<String, ?> readJobOutputs(@NonNull FileEntry fileEntry) {
        Validate.notNull(fileEntry, "'fileEntry' must not be null");

        return JsonUtils.read(
            CompressionUtils.decompressToString(fileStorageService.readFileToBytes(JOB_FILES_DIR, fileEntry)),
            new TypeReference<>() {}, objectMapper);
    }

    @Override
    public Object readTaskExecutionOutput(@NonNull FileEntry fileEntry) {
        Validate.notNull(fileEntry, "'fileEntry' must not be null");

        return JsonUtils.read(
            CompressionUtils.decompressToString(
                fileStorageService.readFileToBytes(TASK_EXECUTION_FILES_DIR, fileEntry)),
            Object.class, objectMapper);
    }

    @Override
    public FileEntry storeContextValue(
        long stackId, @NonNull Context.Classname classname, @NonNull Map<String, ?> value) {

        Validate.notNull(classname, "'classname' must not be null");
        Validate.notNull(value, "'value' must not be null");

        return fileStorageService.storeFileContent(
            CONTEXT_FILES_DIR, classname + "_" + stackId + ".json",
            CompressionUtils.compress(JsonUtils.write(value, objectMapper)));
    }

    @Override
    public FileEntry storeContextValue(
        long stackId, int subStackId, @NonNull Context.Classname classname, @NonNull Map<String, ?> value) {

        Validate.notNull(classname, "'classname' must not be null");
        Validate.notNull(value, "'value' must not be null");

        return fileStorageService.storeFileContent(
            CONTEXT_FILES_DIR, classname + "_" + stackId + "_" + subStackId + ".json",
            CompressionUtils.compress(JsonUtils.write(value, objectMapper)));
    }

    @Override
    public FileEntry storeJobOutputs(long jobId, @NonNull Map<String, ?> outputs) {
        Validate.notNull(outputs, "'outputs' must not be null");

        return fileStorageService.storeFileContent(
            JOB_FILES_DIR, jobId + ".json", CompressionUtils.compress(JsonUtils.write(outputs, objectMapper)));
    }

    @Override
    public FileEntry storeTaskExecutionOutput(long taskExecutionId, @NonNull Object output) {
        Validate.notNull(output, "'output' must not be null");

        return fileStorageService.storeFileContent(
            TASK_EXECUTION_FILES_DIR, taskExecutionId + ".json",
            CompressionUtils.compress(JsonUtils.write(output, objectMapper)));
    }
}
