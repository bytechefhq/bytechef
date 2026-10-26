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

package com.bytechef.platform.component.log.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.EditorLogFileStorage;
import com.bytechef.platform.component.log.EditorLogFileStorageImpl;
import com.bytechef.platform.component.log.JdbcLogFileStorage;
import com.bytechef.platform.component.log.JdbcLogFileStorage.Scope;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.LogFileStorageImpl;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.component.log.TriggerLogFileStorageImpl;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * @author Ivica Cardic
 */
@Configuration
class LogFileStorageConfiguration {

    @Bean
    EditorLogFileStorage editorLogFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry,
        ObjectProvider<NamedParameterJdbcOperations> namedParameterJdbcOperationsProvider) {

        NamedParameterJdbcOperations namedParameterJdbcOperations = getNamedParameterJdbcOperations(
            applicationProperties, namedParameterJdbcOperationsProvider);

        if (namedParameterJdbcOperations != null) {
            return new EditorLogFileStorageImpl(new JdbcLogFileStorage(namedParameterJdbcOperations, Scope.EDITOR));
        }

        return new EditorLogFileStorageImpl(getFileStorageService(applicationProperties, fileStorageServiceRegistry));
    }

    @Bean
    TriggerLogFileStorage triggerLogFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry,
        ObjectProvider<NamedParameterJdbcOperations> namedParameterJdbcOperationsProvider) {

        NamedParameterJdbcOperations namedParameterJdbcOperations = getNamedParameterJdbcOperations(
            applicationProperties, namedParameterJdbcOperationsProvider);

        if (namedParameterJdbcOperations != null) {
            return new TriggerLogFileStorageImpl(new JdbcLogFileStorage(namedParameterJdbcOperations, Scope.TRIGGER));
        }

        return new TriggerLogFileStorageImpl(getFileStorageService(applicationProperties, fileStorageServiceRegistry));
    }

    @Bean
    LogFileStorage logFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry,
        ObjectProvider<NamedParameterJdbcOperations> namedParameterJdbcOperationsProvider) {

        NamedParameterJdbcOperations namedParameterJdbcOperations = getNamedParameterJdbcOperations(
            applicationProperties, namedParameterJdbcOperationsProvider);

        if (namedParameterJdbcOperations != null) {
            return new JdbcLogFileStorage(namedParameterJdbcOperations, Scope.JOB);
        }

        return new LogFileStorageImpl(getFileStorageService(applicationProperties, fileStorageServiceRegistry));
    }

    private static FileStorageService getFileStorageService(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        Provider provider = getProvider(applicationProperties);

        return fileStorageServiceRegistry.getFileStorageService(provider.name());
    }

    private static @Nullable NamedParameterJdbcOperations getNamedParameterJdbcOperations(
        ApplicationProperties applicationProperties,
        ObjectProvider<NamedParameterJdbcOperations> namedParameterJdbcOperationsProvider) {

        if (getProvider(applicationProperties) != Provider.JDBC) {
            return null;
        }

        return namedParameterJdbcOperationsProvider.getIfAvailable();
    }

    private static Provider getProvider(ApplicationProperties applicationProperties) {
        ApplicationProperties.FileStorage fileStorage = applicationProperties.getFileStorage();

        return fileStorage.getProvider();
    }
}
