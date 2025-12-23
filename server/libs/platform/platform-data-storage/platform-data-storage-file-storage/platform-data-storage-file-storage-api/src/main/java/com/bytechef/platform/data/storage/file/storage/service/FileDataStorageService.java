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

import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface FileDataStorageService {

    void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        ModeType type);

    <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, ModeType type);

    <T> T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        ModeType type);

    <T> Map<String, T> getAll(
        String componentName, DataStorageScope scope, String scopeId, long environmentId, ModeType type);

    void put(
        String componentName, DataStorageScope scope, String scopeId, String key, Object value, long environmentId,
        ModeType type);
}
