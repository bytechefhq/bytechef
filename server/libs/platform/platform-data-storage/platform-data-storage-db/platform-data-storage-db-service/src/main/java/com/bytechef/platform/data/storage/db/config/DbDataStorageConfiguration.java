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

package com.bytechef.platform.data.storage.db.config;

import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.data.storage.db.repository.DataStorageRepository;
import com.bytechef.platform.data.storage.db.service.DbDataStorageService;
import com.bytechef.platform.data.storage.db.service.DbDataStorageServiceImpl;
import com.bytechef.platform.data.storage.service.DataStorageService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "data-storage.provider", havingValue = "db")
public class DbDataStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DbDataStorageConfiguration.class);

    public DbDataStorageConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Data storage provider type enabled: db");
        }
    }

    @Bean
    DbDataStorageService dbDataStorageService(DataStorageRepository dataStorageRepository) {
        return new DbDataStorageServiceImpl(dataStorageRepository);
    }

    @Bean
    DataStorageService dataStorageService(DbDataStorageService dbDataStorageService) {
        return new DataStorageServiceImpl(dbDataStorageService);
    }

    private record DataStorageServiceImpl(DbDataStorageService dbDataStorageService) implements DataStorageService {

        @Override
        public <T> Optional<T> fetch(
            String componentName, String actionName, Scope scope, String scopeId, String key,
            Type type) {

            return dbDataStorageService.fetch(componentName, actionName, scope, scopeId, key, type);
        }

        @Override
        public <T> T get(
            String componentName, String actionName, Scope scope, String scopeId, String key,
            Type type) {

            return dbDataStorageService.get(componentName, actionName, scope, scopeId, key, type);
        }

        @Override
        public void put(
            String componentName, String actionName, Scope scope, String scopeId, String key,
            Type type, Object value) {

            dbDataStorageService.put(componentName, actionName, scope, scopeId, key, type, value);
        }
    }
}
