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

package com.bytechef.platform.data.storage.file.storage.service;

import com.bytechef.commons.data.jdbc.wrapper.TypedValue;
import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
public class FileDataStorageServiceImpl implements FileDataStorageService {

    public static final String DATA_ENTRIES_ROOT_DIR = "data_entries/";

    private static final int LOCK_STRIPES = 64;
    private static final ReentrantLock[] LOCKS = IntStream.range(0, LOCK_STRIPES)
        .mapToObj(index -> new ReentrantLock())
        .toArray(ReentrantLock[]::new);

    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public FileDataStorageServiceImpl(FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {
        String directoryPath = getDirectoryPath(type, environmentId);

        fileStorageService.deleteFile(
            directoryPath,
            fileStorageService.getFileEntry(directoryPath, getFilename(componentName, scope, scopeId, key)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        String directoryPath = getDirectoryPath(type, environmentId);

        if (!fileStorageService.fileExists(directoryPath, getFilename(componentName, scope, scopeId, key))) {
            return Optional.empty();
        }

        TypedValue typedValue = TypedValue.read(
            objectMapper,
            CompressionUtils.decompressToString(
                fileStorageService.readFileToBytes(
                    directoryPath,
                    fileStorageService.getFileEntry(directoryPath, getFilename(componentName, scope, scopeId, key)))));

        return Optional.ofNullable((T) typedValue.value());
    }

    @Override
    public <T> T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {
        Optional<T> optional = fetch(componentName, scope, scopeId, key, environmentId, type);

        return optional.orElseThrow(() -> new IllegalArgumentException(
            "No value found for component: " + componentName +
                ", scope: " + scope +
                ", scopeId: " + scopeId +
                ", key: " + key +
                ", type: " + type));
    }

    @Override
    public <T> Map<String, T> getAll(
        String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {
        String prefix = getFilename(componentName, scope, scopeId, "");

        return fileStorageService.getFileEntries(getDirectoryPath(type, environmentId))
            .stream()
            .map(FileEntry::getName)
            .filter(filename -> filename.startsWith(prefix))
            .map(filename -> filename.substring(prefix.length()))
            .collect(
                Collectors.toMap(key -> key, key -> get(componentName, scope, scopeId, key, environmentId, type)));
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, Object value, long environmentId,
        PlatformType type) {

        TypedValue typedValue = new TypedValue(value);

        fileStorageService.storeFileContent(
            getDirectoryPath(type, environmentId), getFilename(componentName, scope, scopeId, key),
            CompressionUtils.compress(typedValue.write(objectMapper)), false);
    }

    @Override
    public <T> T update(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type, Function<Optional<T>, T> updater) {

        ReentrantLock lock = getLock(
            getDirectoryPath(type, environmentId) + "/" + getFilename(componentName, scope, scopeId, key));

        lock.lock();

        try {
            T value = updater.apply(fetch(componentName, scope, scopeId, key, environmentId, type));

            if (value == null) {
                throw new IllegalArgumentException("A data storage update must not return null");
            }

            put(componentName, scope, scopeId, key, value, environmentId, type);

            return value;
        } finally {
            lock.unlock();
        }
    }

    private static ReentrantLock getLock(String lockKey) {
        return LOCKS[Math.floorMod(lockKey.hashCode(), LOCK_STRIPES)];
    }

    private static String getDirectoryPath(PlatformType type, long environmentId) {
        return DATA_ENTRIES_ROOT_DIR + type.ordinal() + "/" + environmentId;
    }

    private static String getFilename(String componentName, DataStorageScope scope, String scopeId, String key) {
        return componentName + "_" + scope.ordinal() + "_" + scopeId + "_" + key;
    }
}
