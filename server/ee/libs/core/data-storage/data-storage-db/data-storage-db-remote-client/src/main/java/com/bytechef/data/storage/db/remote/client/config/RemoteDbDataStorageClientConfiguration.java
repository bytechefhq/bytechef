
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.data.storage.db.remote.client.config;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.data.storage.db.remote.client.service.RemoteDbDataStorageServiceClient;
import com.bytechef.data.storage.db.service.DbDataStorageService;
import com.bytechef.data.storage.service.DataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "data-storage.provider", havingValue = "db")
public class RemoteDbDataStorageClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RemoteDbDataStorageClientConfiguration.class);

    public RemoteDbDataStorageClientConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Data storage provider type enabled: db");
        }
    }

    @Bean
    DataStorageService dataStorageService(DbDataStorageService dbDataStorageService) {
        return new DataStorageServiceImpl(dbDataStorageService);
    }

    @Bean
    DbDataStorageService dbDataStorageService(LoadBalancedWebClient loadBalancedWebClient) {
        return new RemoteDbDataStorageServiceClient(loadBalancedWebClient);
    }

    private record DataStorageServiceImpl(DbDataStorageService DbDataStorageService)
        implements DataStorageService {

        @Override
        public <T> Optional<T> fetch(String context, int scope, long scopeId, String key) {
            return DbDataStorageService.fetch(context, scope, scopeId, key);
        }

        @Override
        public <T> T get(String context, int scope, long scopeId, String key) {
            return DbDataStorageService.get(context, scope, scopeId, key);
        }

        @Override
        public void put(String context, int scope, long scopeId, String key, Object value) {
            DbDataStorageService.put(context, scope, scopeId, key, value);
        }
    }
}
