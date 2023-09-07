
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.file.storage;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.InputStream;
import java.util.Map;

public class WorkflowFileStorageImpl implements WorkflowFileStorage {

    private static final String WORKFLOWS_CONTEXTS = "workflows/contexts";
    private static final String WORKFLOWS_DATA = "workflows/data";
    private static final String WORKFLOWS_JOBS = "workflows/jobs";
    private static final String WORKFLOWS_TASK_EXECUTIONS = "workflows/task_executions";

    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public WorkflowFileStorageImpl(FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public InputStream getFileStream(String filename, String url) {
        return fileStorageService.getFileStream(WORKFLOWS_DATA, new FileEntry(WORKFLOWS_DATA, filename, url));
    }

    @Override
    public Map<String, ?> readContextValue(FileEntry fileEntry) {
        return JsonUtils.read(
            fileStorageService.readFileToString(WORKFLOWS_CONTEXTS, fileEntry), new TypeReference<>() {}, objectMapper);
    }

    @Override
    public String readFileToString(String filename, String url) {
        return fileStorageService.readFileToString(WORKFLOWS_DATA, new FileEntry(WORKFLOWS_DATA, filename, url));
    }

    @Override
    public Map<String, ?> readJobOutputs(FileEntry fileEntry) {
        return JsonUtils.read(
            fileStorageService.readFileToString(WORKFLOWS_JOBS, fileEntry), new TypeReference<>() {}, objectMapper);
    }

    @Override
    public Object readTaskExecutionOutput(FileEntry fileEntry) {
        return JsonUtils.read(
            fileStorageService.readFileToString(WORKFLOWS_TASK_EXECUTIONS, fileEntry), Object.class, objectMapper);
    }

    @Override
    public FileEntry storeContextValue(long stackId, Context.Classname classname, Map<String, ?> value) {
        return fileStorageService.storeFileContent(
            WORKFLOWS_CONTEXTS, classname + "_" + stackId + ".json", JsonUtils.write(value, objectMapper));
    }

    @Override
    public FileEntry storeContextValue(
        long stackId, int subStackId, Context.Classname classname, Map<String, ?> value) {

        return fileStorageService.storeFileContent(
            WORKFLOWS_CONTEXTS, classname + "_" + stackId + "_" + subStackId + ".json",
            JsonUtils.write(value, objectMapper));
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) {
        return fileStorageService.storeFileContent(WORKFLOWS_DATA, fileName, data);
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        return fileStorageService.storeFileContent(WORKFLOWS_DATA, fileName, inputStream);
    }

    @Override
    public FileEntry storeJobOutputs(long jobId, Map<String, ?> outputs) {
        return fileStorageService.storeFileContent(WORKFLOWS_JOBS, jobId + ".json",
            JsonUtils.write(outputs, objectMapper));
    }

    @Override
    public FileEntry storeTaskExecutionOutput(Long taskExecutionId, Object output) {
        return fileStorageService.storeFileContent(
            WORKFLOWS_TASK_EXECUTIONS, taskExecutionId + ".json", JsonUtils.write(output, objectMapper));
    }
}
