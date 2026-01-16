/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.config;

import com.bytechef.ee.platform.data.storage.db.remote.client.service.RemoteJdbcDataStorageServiceClient;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.annotation.ConditionalOnDataStorageProviderJdbc;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
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
@ConditionalOnDataStorageProviderJdbc
public class RemoteJdbcDataStorageClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RemoteJdbcDataStorageClientConfiguration.class);

    public RemoteJdbcDataStorageClientConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Data storage provider type enabled: db");
        }
    }

    @Bean
    DataStorage dataStorageService(JdbcDataStorageService jdbcDataStorageService) {
        return new DataStorageImpl(jdbcDataStorageService);
    }

    @Bean
    JdbcDataStorageService jdbcDataStorageService(LoadBalancedRestClient loadBalancedRestClient) {
        return new RemoteJdbcDataStorageServiceClient(loadBalancedRestClient);
    }

    private record DataStorageImpl(JdbcDataStorageService jdbcDataStorageService) implements DataStorage {

        @Override
        public <T> Optional<T> fetch(
            String componentName, DataStorageScope scope, String scopeId,
            String key, long environmentId, PlatformType type) {

            return jdbcDataStorageService.fetch(componentName, scope, scopeId, key, environmentId, type);
        }

        @Override
        public <T> T get(
            String componentName, DataStorageScope scope, String scopeId,
            String key, long environmentId, PlatformType type) {

            return jdbcDataStorageService.get(componentName, scope, scopeId, key, environmentId, type);
        }

        @Override
        public <T> Map<String, T> getAll(
            String componentName, DataStorageScope scope, String scopeId, long environmentId,
            PlatformType type) {

            return jdbcDataStorageService.getAll(componentName, scope, scopeId, environmentId, type);
        }

        @Override
        public void put(
            String componentName, DataStorageScope scope, String scopeId,
            String key, Object value, long environmentId, PlatformType type) {

            jdbcDataStorageService.put(componentName, scope, scopeId, key, environmentId, type, value);
        }

        @Override
        public void delete(
            String componentName, DataStorageScope scope, String scopeId,
            String key, long environmentId, PlatformType type) {

            jdbcDataStorageService.delete(componentName, scope, scopeId, key, environmentId, type);
        }
    }
}
