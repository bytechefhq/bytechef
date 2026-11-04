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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.IntStream;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
public class FileDataStorageServiceImpl implements FileDataStorageService {

    public static final String LEGACY_DATA_ENTRIES_ROOT_DIR = "data_entries/";
    public static final String DATA_STORAGE_ROOT_DIR = "data_storage/";

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

        String directoryPath = getDirectoryPath(type, scope, scopeId, environmentId);
        String filename = getFilename(componentName, key);
        ReentrantLock lock = getLock(directoryPath + "/" + filename);

        lock.lock();

        try {
            deleteIfExists(directoryPath, filename);
            deleteIfExists(
                getLegacyDirectoryPath(type, environmentId), getLegacyFilename(componentName, scope, scopeId, key));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
        String scopeDirectoryPath = getScopeDirectoryPath(type, scope, scopeId);

        for (FileEntry fileEntry : fileStorageService.getFileEntries(scopeDirectoryPath)) {
            fileStorageService.deleteFile(scopeDirectoryPath, fileEntry);
        }
    }

    @Override
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        String directoryPath = getDirectoryPath(type, scope, scopeId, environmentId);
        String filename = getFilename(componentName, key);

        if (fileStorageService.fileExists(directoryPath, filename)) {
            return Optional.ofNullable(read(directoryPath, fileStorageService.getFileEntry(directoryPath, filename)));
        }

        String legacyDirectoryPath = getLegacyDirectoryPath(type, environmentId);
        String legacyFilename = getLegacyFilename(componentName, scope, scopeId, key);

        if (fileStorageService.fileExists(legacyDirectoryPath, legacyFilename)) {
            return Optional.ofNullable(
                read(legacyDirectoryPath, fileStorageService.getFileEntry(legacyDirectoryPath, legacyFilename)));
        }

        return Optional.empty();
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

        Map<String, T> values = new HashMap<>();

        String legacyDirectoryPath = getLegacyDirectoryPath(type, environmentId);
        String legacyPrefix = getLegacyFilename(componentName, scope, scopeId, "");

        for (FileEntry fileEntry : fileStorageService.getFileEntries(legacyDirectoryPath)) {
            String filename = fileEntry.getName();

            if (filename.startsWith(legacyPrefix)) {
                values.put(filename.substring(legacyPrefix.length()), read(legacyDirectoryPath, fileEntry));
            }
        }

        String directoryPath = getDirectoryPath(type, scope, scopeId, environmentId);
        String prefix = getFilename(componentName, "");

        for (FileEntry fileEntry : fileStorageService.getFileEntries(directoryPath)) {
            String filename = fileEntry.getName();

            if (filename.startsWith(prefix)) {
                values.put(filename.substring(prefix.length()), read(directoryPath, fileEntry));
            }
        }

        return values;
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, Object value, long environmentId,
        PlatformType type) {

        TypedValue typedValue = new TypedValue(value);
        String directoryPath = getDirectoryPath(type, scope, scopeId, environmentId);
        String filename = getFilename(componentName, key);
        ReentrantLock lock = getLock(directoryPath + "/" + filename);

        lock.lock();

        try {
            fileStorageService.storeFileContent(
                directoryPath, filename, CompressionUtils.compress(typedValue.write(objectMapper)), false);

            deleteIfExists(
                getLegacyDirectoryPath(type, environmentId), getLegacyFilename(componentName, scope, scopeId, key));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T update(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type, Function<Optional<T>, T> updater) {

        ReentrantLock lock = getLock(
            getDirectoryPath(type, scope, scopeId, environmentId) + "/" + getFilename(componentName, key));

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

    private void deleteIfExists(String directoryPath, String filename) {
        if (fileStorageService.fileExists(directoryPath, filename)) {
            fileStorageService.deleteFile(directoryPath, fileStorageService.getFileEntry(directoryPath, filename));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T read(String directoryPath, FileEntry fileEntry) {
        TypedValue typedValue = TypedValue.read(
            objectMapper,
            CompressionUtils.decompressToString(fileStorageService.readFileToBytes(directoryPath, fileEntry)));

        return (T) typedValue.value();
    }

    private static ReentrantLock getLock(String lockKey) {
        return LOCKS[Math.floorMod(lockKey.hashCode(), LOCK_STRIPES)];
    }

    private static String getDirectoryPath(
        PlatformType type, DataStorageScope scope, String scopeId, long environmentId) {

        return getScopeDirectoryPath(type, scope, scopeId) + "/" + environmentId;
    }

    private static String getFilename(String componentName, String key) {
        return componentName + "_" + key;
    }

    private static String getLegacyDirectoryPath(PlatformType type, long environmentId) {
        return LEGACY_DATA_ENTRIES_ROOT_DIR + type.ordinal() + "/" + environmentId;
    }

    private static String getLegacyFilename(String componentName, DataStorageScope scope, String scopeId, String key) {
        return componentName + "_" + scope.ordinal() + "_" + scopeId + "_" + key;
    }

    private static String getScopeDirectoryPath(PlatformType type, DataStorageScope scope, String scopeId) {
        return DATA_STORAGE_ROOT_DIR + type.ordinal() + "/" + scope.ordinal() + "/" + scopeId;
    }
}
