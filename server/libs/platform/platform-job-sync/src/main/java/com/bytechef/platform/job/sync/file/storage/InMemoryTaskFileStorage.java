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

package com.bytechef.platform.job.sync.file.storage;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory TaskFileStorage implementation that stores objects directly without serialization or compression, using
 * job-scoped storage. This is useful for testing scenarios where you want to preserve the exact objects (like Publisher
 * instances) without JSON serialization, isolated per job execution.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class InMemoryTaskFileStorage implements TaskFileStorage {

    private static final String CONTEXT_FILES_DIR = "outputs/workflow_contexts";
    private static final String JOB_FILES_DIR = "outputs/workflow_jobs";
    private static final String TASK_EXECUTION_FILES_DIR = "outputs/workflow_task_executions";
    private static final String URL_PREFIX = "inmemory://";

    private final Map<Long, Map<String, Object>> jobDataStorage = new ConcurrentHashMap<>();

    public void initializeJobStorage(long jobId) {
        jobDataStorage.putIfAbsent(jobId, new ConcurrentHashMap<>());
    }

    public void cleanupJobStorage(long jobId) {
        jobDataStorage.remove(jobId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> readContextValue(FileEntry fileEntry) {
        Object data = readObject(fileEntry);

        if (!(data instanceof Map)) {
            throw new FileStorageException("Expected Map but found: " + data.getClass());
        }

        return (Map<String, ?>) data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> readJobOutputs(FileEntry fileEntry) {
        Object data = readObject(fileEntry);

        if (!(data instanceof Map)) {
            throw new FileStorageException("Expected Map but found: " + data.getClass());
        }

        return (Map<String, ?>) data;
    }

    @Override
    public Object readTaskExecutionOutput(FileEntry fileEntry) {
        return readObject(fileEntry);
    }

    @Override
    public FileEntry storeContextValue(
        long stackId, Context.Classname classname, Map<String, ?> value) {

        String filename = stackId + "_" + classname.name() + ".dat";

        return storeObject(CONTEXT_FILES_DIR, filename, value, stackId);
    }

    @Override
    public FileEntry storeContextValue(
        long stackId, int subStackId, Context.Classname classname, Map<String, ?> value) {

        String filename = stackId + "_" + subStackId + "_" + classname.name() + ".dat";

        return storeObject(CONTEXT_FILES_DIR, filename, value, stackId);
    }

    @Override
    public FileEntry storeJobOutputs(long jobId, Map<String, ?> outputs) {
        String filename = jobId + ".dat";

        return storeObject(JOB_FILES_DIR, filename, outputs, jobId);
    }

    @Override
    public FileEntry storeTaskExecutionOutput(long jobId, long taskExecutionId, Object output) {
        String filename = taskExecutionId + ".dat";

        return storeObject(TASK_EXECUTION_FILES_DIR, filename, output, jobId);
    }

    private Object readObject(FileEntry fileEntry) {
        UrlParts urlParts = extractJobIdAndKeyFromUrl(fileEntry.getUrl());
        Map<String, Object> storage = jobDataStorage.get(urlParts.jobId());

        if (storage == null || !storage.containsKey(urlParts.key())) {
            throw new FileStorageException("Data not found: " + fileEntry.getName());
        }

        return storage.get(urlParts.key());
    }

    private FileEntry storeObject(String directory, String filename, Object data, long jobId) {
        Map<String, Object> storage = jobDataStorage.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>());
        String key = getKey(directory, filename);

        // ConcurrentHashMap doesn't allow null values, so wrap it
        storage.put(key, data);

        // Encode job ID in URL so we can retrieve it later
        return new FileEntry(filename, URL_PREFIX + jobId + "/" + key);
    }

    private static String getKey(String directory, String filename) {
        return directory + "/" + filename;
    }

    private static UrlParts extractJobIdAndKeyFromUrl(String url) {
        if (!url.startsWith(URL_PREFIX)) {
            throw new FileStorageException("Invalid URL format: " + url);
        }

        String remainder = url.substring(URL_PREFIX.length());
        int slashIndex = remainder.indexOf('/');

        if (slashIndex == -1) {
            throw new FileStorageException("Invalid URL format - missing job ID: " + url);
        }

        try {
            long jobId = Long.parseLong(remainder.substring(0, slashIndex));
            String key = remainder.substring(slashIndex + 1);

            return new UrlParts(jobId, key);
        } catch (NumberFormatException e) {
            throw new FileStorageException("Invalid job ID in URL: " + url, e);
        }
    }

    private record UrlParts(long jobId, String key) {
    }
}
