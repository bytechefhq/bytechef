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

package com.bytechef.platform.data.storage.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.annotation.ConditionalOnDataStorageProviderAws;
import com.bytechef.platform.data.storage.annotation.ConditionalOnDataStorageProviderFilesystem;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.file.storage.service.DataFileStorageService;
import com.bytechef.platform.data.storage.file.storage.service.DataFileStorageServiceImpl;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DataFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DataFileStorageConfiguration.class);

    @Bean
    @ConditionalOnDataStorageProviderAws
    DataStorage awsFileStorageDataStorageService(FileStorageServiceRegistry fileStorageServiceRegistry) {
        if (logger.isInfoEnabled()) {
            logger.info("Data storage provider type enabled: aws");
        }

        return new DataStorageImpl(new DataFileStorageServiceImpl(
            fileStorageServiceRegistry.getFileStorageService(ApplicationProperties.DataStorage.Provider.AWS.name())));
    }

    @Bean
    @ConditionalOnDataStorageProviderFilesystem
    DataStorage filesystemFileStorageDataStorageService(FileStorageServiceRegistry fileStorageServiceRegistry) {
        if (logger.isInfoEnabled()) {
            logger.info("Data storage provider type enabled: filesystem");
        }

        return new DataStorageImpl(
            new DataFileStorageServiceImpl(
                fileStorageServiceRegistry.getFileStorageService(
                    ApplicationProperties.DataStorage.Provider.FILESYSTEM.name())));
    }

    private record DataStorageImpl(DataFileStorageService dataFileStorageService) implements DataStorage {

        @NonNull
        @Override
        public <T> Optional<T> fetch(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull ModeType type) {

            return dataFileStorageService.fetch(componentName, scope, scopeId, key, type);
        }

        @NonNull
        @Override
        public <T> T get(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull ModeType type) {

            return dataFileStorageService.get(componentName, scope, scopeId, key, type);
        }

        @NonNull
        @Override
        public <T> Map<String, T> getAll(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull ModeType type) {

            return dataFileStorageService.getAll(componentName, scope, scopeId, type);
        }

        @Override
        public void put(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull ModeType type, @NonNull Object value) {

            dataFileStorageService.put(componentName, scope, scopeId, key, type, value);
        }

        @Override
        public void delete(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull ModeType type) {

            dataFileStorageService.delete(componentName, scope, scopeId, key, type);
        }
    }
}
