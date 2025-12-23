/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.config;

import com.bytechef.ee.platform.data.storage.db.remote.client.service.RemoteJdbcDataStorageServiceClient;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.ModeType;
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
import org.springframework.lang.NonNull;

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

        @NonNull
        @Override
        public <T> Optional<T> fetch(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, long environmentId, @NonNull ModeType type) {

            return jdbcDataStorageService.fetch(componentName, scope, scopeId, key, environmentId, type);
        }

        @NonNull
        @Override
        public <T> T get(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, long environmentId, @NonNull ModeType type) {

            return jdbcDataStorageService.get(componentName, scope, scopeId, key, environmentId, type);
        }

        @NonNull
        @Override
        public <T> Map<String, T> getAll(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, long environmentId,
            @NonNull ModeType type) {

            return jdbcDataStorageService.getAll(componentName, scope, scopeId, environmentId, type);
        }

        @Override
        public void put(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, @NonNull Object value, long environmentId, @NonNull ModeType type) {

            jdbcDataStorageService.put(componentName, scope, scopeId, key, environmentId, type, value);
        }

        @Override
        public void delete(
            @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
            @NonNull String key, long environmentId, @NonNull ModeType type) {

            jdbcDataStorageService.delete(componentName, scope, scopeId, key, environmentId, type);
        }
    }
}
