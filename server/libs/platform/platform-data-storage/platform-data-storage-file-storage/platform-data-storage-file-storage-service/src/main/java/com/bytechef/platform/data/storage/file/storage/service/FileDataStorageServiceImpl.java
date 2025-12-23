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

import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.domain.ValueWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class FileDataStorageServiceImpl implements FileDataStorageService {

    public static final String DATA_ENTRIES_ROOT_DIR = "data_entries/";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public FileDataStorageServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        ModeType type) {
        String directoryPath = getDirectoryPath(type, environmentId);

        fileStorageService.deleteFile(
            directoryPath,
            fileStorageService.getFileEntry(directoryPath, getFilename(componentName, scope, scopeId, key)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, ModeType type) {

        String directoryPath = getDirectoryPath(type, environmentId);

        if (!fileStorageService.fileExists(directoryPath, getFilename(componentName, scope, scopeId, key))) {
            return Optional.empty();
        }

        ValueWrapper valueWrapper = ValueWrapper.read(
            CompressionUtils.decompressToString(
                fileStorageService.readFileToBytes(
                    directoryPath,
                    fileStorageService.getFileEntry(directoryPath, getFilename(componentName, scope, scopeId, key)))));

        return Optional.ofNullable((T) valueWrapper.value());
    }

    @Override
    public <T> T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, ModeType type) {
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
        String componentName, DataStorageScope scope, String scopeId, long environmentId, ModeType type) {
        return fileStorageService.getFileEntries(getDirectoryPath(type, environmentId))
            .stream()
            .collect(
                Collectors.toMap(
                    fileEntry -> getKey(fileEntry.getName()),
                    fileEntry -> get(
                        componentName, scope, scopeId, getKey(fileEntry.getName()), environmentId, type)));
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, Object value, long environmentId,
        ModeType type) {

        ValueWrapper valueWrapper = new ValueWrapper(value);

        fileStorageService.storeFileContent(
            getDirectoryPath(type, environmentId), getFilename(componentName, scope, scopeId, key),
            CompressionUtils.compress(valueWrapper.write()), false);
    }

    private static String getDirectoryPath(ModeType type, long environmentId) {
        return DATA_ENTRIES_ROOT_DIR + type.ordinal() + "/" + environmentId;
    }

    private static String getFilename(String componentName, DataStorageScope scope, String scopeId, String key) {
        return componentName + "_" + scope.ordinal() + "_" + scopeId + "_" + key;
    }

    private String getKey(String filename) {
        String[] items = filename.split("_");

        return items[3];
    }
}
