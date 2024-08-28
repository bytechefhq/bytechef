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

package com.bytechef.platform.data.storage.jdbc.config;

import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.annotation.ConditionalOnDataStorageProviderJdbc;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageServiceImpl;
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
@ConditionalOnDataStorageProviderJdbc
public class JdbcDataStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JdbcDataStorageConfiguration.class);

    public JdbcDataStorageConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Data storage provider type enabled: jdbc");
        }
    }

    @Bean
    JdbcDataStorageService jdbcDataStorageService(DataStorageRepository dataStorageRepository) {
        return new JdbcDataStorageServiceImpl(dataStorageRepository);
    }

    @Bean
    DataStorage dataStorageService(JdbcDataStorageService jdbcDataStorageService) {
        return new DataStorageImpl(jdbcDataStorageService);
    }

    private record DataStorageImpl(JdbcDataStorageService jdbcDataStorageService) implements DataStorage {

        @NonNull
        @Override
        public <T> Optional<T> fetch(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull AppType type) {

            return jdbcDataStorageService.fetch(componentName, scope, scopeId, key, type);
        }

        @NonNull
        @Override
        public <T> T get(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull AppType type) {

            return jdbcDataStorageService.get(componentName, scope, scopeId, key, type);
        }

        @NonNull
        @Override
        public <T> Map<String, T> getAll(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull AppType type) {

            return jdbcDataStorageService.getAll(componentName, scope, scopeId, type);
        }

        @Override
        public void put(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull AppType type, @NonNull Object value) {

            jdbcDataStorageService.put(componentName, scope, scopeId, key, type, value);
        }

        @Override
        public void delete(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull AppType type) {

            jdbcDataStorageService.delete(componentName, scope, scopeId, key, type);
        }
    }
}
