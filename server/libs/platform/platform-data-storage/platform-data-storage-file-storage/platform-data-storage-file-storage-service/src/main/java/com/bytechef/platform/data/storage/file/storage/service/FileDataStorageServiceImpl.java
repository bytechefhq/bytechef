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

package com.bytechef.platform.data.storage.file.storage.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.domain.ValueWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class FileDataStorageServiceImpl implements FileDataStorageService {

    public static final String DATA_ENTRIES_DIR = "data_entries";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public FileDataStorageServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void delete(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type) {

        String directoryPath = getDirectoryPath(type);

        fileStorageService.deleteFile(
            directoryPath,
            fileStorageService.getFileEntry(directoryPath, getFilename(componentName, scope, scopeId, key)));
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetch(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type) {

        String directoryPath = getDirectoryPath(type);

        if (!fileStorageService.fileExists(directoryPath, getFilename(componentName, scope, scopeId, key))) {
            return Optional.empty();
        }

        ValueWrapper valueWrapper = ValueWrapper.read(
            fileStorageService.readFileToString(
                directoryPath,
                fileStorageService.getFileEntry(directoryPath, getFilename(componentName, scope, scopeId, key))));

        return Optional.ofNullable((T) valueWrapper.value());
    }

    @NonNull
    @Override
    public <T> T get(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type) {

        return OptionalUtils.get(fetch(componentName, scope, scopeId, key, type));
    }

    @NonNull
    @Override
    public <T> Map<String, T> getAll(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
        @NonNull AppType type) {

        return fileStorageService.getFileEntries(getDirectoryPath(type), getDirectoryPath(type))
            .stream()
            .collect(Collectors.toMap(
                fileEntry -> getKey(fileEntry.getName()),
                fileEntry -> get(componentName, scope, scopeId, getKey(fileEntry.getName()), type)));
    }

    @Override
    public void put(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type, @NonNull Object value) {

        ValueWrapper valueWrapper = new ValueWrapper(value);

        fileStorageService.storeFileContent(
            getDirectoryPath(type), getFilename(componentName, scope, scopeId, key), valueWrapper.write(), false);
    }

    private static String getDirectoryPath(AppType type) {
        return DATA_ENTRIES_DIR + '_' + type.ordinal();
    }

    private static String getFilename(String componentName, DataStorageScope scope, String scopeId, String key) {
        return componentName + "_" + scope.ordinal() + "_" + scopeId + "_" + key;
    }

    private String getKey(String filename) {
        String[] items = filename.split("_");

        return items[3];
    }
}
