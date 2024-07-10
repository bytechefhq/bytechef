/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.service;

import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.AppType;
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
    public void delete(String componentName, Scope scope, String scopeId, String key, AppType type) {
        loadBalancedRestClient.delete(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/delete/{componentName}/{scope}/{scopeId}/{key}/{type}")
                .build(componentName, scope, scopeId, key, type));
    }

    @Override
    public <T> Optional<T> fetch(String componentName, Scope scope, String scopeId, String key, AppType type) {
        return fetchValue(componentName, scope, scopeId, key, type);
    }

    @Override
    public <T> T get(String componentName, Scope scope, String scopeId, String key, AppType type) {
        Optional<T> valueOptional = fetchValue(componentName, scope, scopeId, key, type);

        return valueOptional.orElseThrow();
    }

    @Override
    public <T> Map<String, T> getAll(String componentName, Scope scope, String scopeId, AppType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(String componentName, Scope scope, String scopeId, String key, AppType type, Object value) {
        loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/save/{componentName}/{scope}/{scopeId}/{key}/{type}")
                .build(componentName, scope, scopeId, key, type),
            value);
    }

    private <T> Optional<T> fetchValue(String componentName, Scope scope, String scopeId, String key, AppType type) {
        return Optional.ofNullable(loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE + "/fetch-value/{componentName}/{scope}/{scopeId}/{key}/{type}")
                .build(componentName, scope, scopeId, key, type),
            new ParameterizedTypeReference<>() {}));
    }
}
