/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.config;

import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.ee.platform.data.storage.db.remote.client.service.RemoteJdbcDataStorageServiceClient;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.data.storage.annotation.ConditionalOnDataStorageProviderDb;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import com.bytechef.platform.data.storage.service.DataStorageService;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnDataStorageProviderDb
public class RemoteJdbcDataStorageClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RemoteJdbcDataStorageClientConfiguration.class);

    public RemoteJdbcDataStorageClientConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Data storage provider type enabled: db");
        }
    }

    @Bean
    DataStorageService dataStorageService(JdbcDataStorageService dbDataStorageService) {
        return new DataStorageServiceImpl(dbDataStorageService);
    }

    @Bean
    JdbcDataStorageService dbDataStorageService(LoadBalancedRestClient loadBalancedRestClient) {
        return new RemoteJdbcDataStorageServiceClient(loadBalancedRestClient);
    }

    private record DataStorageServiceImpl(JdbcDataStorageService jdbcDataStorageService)
        implements DataStorageService {

        @Override
        public <T> Optional<T> fetch(
            String componentName, Scope scope, String scopeId, String key,
            AppType type) {

            return jdbcDataStorageService.fetch(componentName, scope, scopeId, key, type);
        }

        @Override
        public <T> T get(
            String componentName, Scope scope, String scopeId, String key,
            AppType type) {

            return jdbcDataStorageService.get(componentName, scope, scopeId, key, type);
        }

        @Override
        public <T> Map<String, T> getAll(String componentName, Scope scope, String scopeId, AppType type) {
            return jdbcDataStorageService.getAll(componentName, scope, scopeId, type);
        }

        @Override
        public void put(
            String componentName, Scope scope, String scopeId, String key,
            AppType type, Object value) {

            jdbcDataStorageService.put(componentName, scope, scopeId, key, type, value);
        }

        @Override
        public void delete(String componentName, Scope scope, String scopeId, String key, AppType type) {
            jdbcDataStorageService.delete(componentName, scope, scopeId, key, type);
        }
    }
}
