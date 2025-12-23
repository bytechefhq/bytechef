/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoteJdbcDataStorageServiceClient implements JdbcDataStorageService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String DATA_STORAGE_SERVICE = "/remote/jdbc-data-storage-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteJdbcDataStorageServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {
        loadBalancedRestClient.delete(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/delete/{componentName}/{scope}/{scopeId}/{key}/{environment}/{type}")
                .build(componentName, scope, scopeId, key, environmentId, type));
    }

    @Override
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        return fetchValue(componentName, scope, scopeId, key, environmentId, type);
    }

    @Override
    public <T> T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {
        Optional<T> valueOptional = fetchValue(componentName, scope, scopeId, key, environmentId, type);

        return valueOptional.orElseThrow();
    }

    @Override
    public <T> Map<String, T> getAll(
        String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void
        put(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type,
            Object value) {
        loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/save/{componentName}/{scope}/{scopeId}/{key}/{environment}/{type}")
                .build(componentName, scope, scopeId, key, environmentId, type),
            value);
    }

    private <T> Optional<T>
        fetchValue(
            String componentName, DataStorageScope scope, String scopeId, String key, long environment,
            PlatformType type) {
        return Optional.ofNullable(loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/fetch-value/{componentName}/{scope}/{scopeId}/{key}/{environment}/{type}")
                .build(componentName, scope, scopeId, key, environment, type),
            new ParameterizedTypeReference<>() {}));
    }
}
