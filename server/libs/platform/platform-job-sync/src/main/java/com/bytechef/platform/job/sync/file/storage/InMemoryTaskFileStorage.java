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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

/**
 * Read-through cache layered over a durable {@link TaskFileStorage}. Used by the synchronous job executors (webhook
 * sync, subflow sync, MCP sync) and by workflow test mode.
 * <p>
 * Every write is forwarded to the durable delegate; the {@link FileEntry} URL returned by the delegate becomes the
 * cache key so that any read that follows in the same sync execution skips the gzip + base64 + Jackson round-trip the
 * durable storage would otherwise do. Reads miss → delegate → fill cache. Sync executors run against real JDBC-backed
 * {@code JobService} / {@code TaskExecutionService} / {@code ContextService}, so these {@link FileEntry} URLs land in
 * the {@code Job.outputs}, {@code TaskExecution.output} and {@code Context.value} columns — durable storage is
 * mandatory here so the async read path (the UI) can later read those columns through the registry-configured
 * {@code FileStorageService}.
 * <p>
 * The cache uses Caffeine's {@code expireAfterAccess(30, MINUTES)} as a bounded-memory safety net; it's in-process
 * only. Caffeine is used directly (not via Spring {@code CacheManager}) so a Redis swap can't break this cache — the
 * values are arbitrary deserialized {@code Map}/{@code Object} instances, some of which aren't {@code Serializable}.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class InMemoryTaskFileStorage implements TaskFileStorage {

    private final Cache<String, Object> jobDataStorage = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();

    private final TaskFileStorage durableTaskFileStorage;

    public InMemoryTaskFileStorage(TaskFileStorage durableTaskFileStorage) {
        this.durableTaskFileStorage = Objects.requireNonNull(durableTaskFileStorage, "durableTaskFileStorage");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> readContextValue(FileEntry fileEntry) {
        Object cached = jobDataStorage.getIfPresent(fileEntry.getUrl());

        if (cached instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }

        Map<String, ?> value = durableTaskFileStorage.readContextValue(fileEntry);

        cache(fileEntry, value);

        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> readJobOutputs(FileEntry fileEntry) {
        Object cached = jobDataStorage.getIfPresent(fileEntry.getUrl());

        if (cached instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }

        Map<String, ?> value = durableTaskFileStorage.readJobOutputs(fileEntry);

        cache(fileEntry, value);

        return value;
    }

    @Override
    public Object readTaskExecutionOutput(FileEntry fileEntry) {
        Object cached = jobDataStorage.getIfPresent(fileEntry.getUrl());

        if (cached != null) {
            return cached;
        }

        Object value = durableTaskFileStorage.readTaskExecutionOutput(fileEntry);

        cache(fileEntry, value);

        return value;
    }

    @Override
    public FileEntry storeContextValue(long stackId, Context.Classname classname, Map<String, ?> value) {
        FileEntry fileEntry = durableTaskFileStorage.storeContextValue(stackId, classname, value);

        cache(fileEntry, value);

        return fileEntry;
    }

    @Override
    public FileEntry storeContextValue(
        long stackId, int subStackId, Context.Classname classname, Map<String, ?> value) {

        FileEntry fileEntry = durableTaskFileStorage.storeContextValue(stackId, subStackId, classname, value);

        cache(fileEntry, value);

        return fileEntry;
    }

    @Override
    public FileEntry storeJobOutputs(long jobId, Map<String, ?> outputs) {
        FileEntry fileEntry = durableTaskFileStorage.storeJobOutputs(jobId, outputs);

        cache(fileEntry, outputs);

        return fileEntry;
    }

    @Override
    public FileEntry storeTaskExecutionOutput(long jobId, long taskExecutionId, Object output) {
        FileEntry fileEntry = durableTaskFileStorage.storeTaskExecutionOutput(jobId, taskExecutionId, output);

        cache(fileEntry, output);

        return fileEntry;
    }

    private void cache(FileEntry fileEntry, @Nullable Object value) {
        if (value == null) {
            return;
        }

        jobDataStorage.put(fileEntry.getUrl(), value);
    }
}
