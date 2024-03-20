/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.data.storage.db.remote.client.config;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.data.storage.db.remote.client.service.RemoteDbDataStorageServiceClient;
import com.bytechef.data.storage.db.service.DbDataStorageService;
import com.bytechef.data.storage.service.DataStorageService;
import com.bytechef.platform.constant.Type;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
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
    DbDataStorageService dbDataStorageService(LoadBalancedRestClient loadBalancedRestClient) {
        return new RemoteDbDataStorageServiceClient(loadBalancedRestClient);
    }

    private record DataStorageServiceImpl(DbDataStorageService dbDataStorageService)
        implements DataStorageService {

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
