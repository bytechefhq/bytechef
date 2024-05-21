/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.data.storage.db.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.data.storage.db.service.DbDataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoteDbDataStorageServiceClient implements DbDataStorageService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String DATA_STORAGE_SERVICE = "/remote/db-ddata-storage-service";
    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteDbDataStorageServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public <T> Optional<T> fetch(
        String componentName, Scope scope, String scopeId, String key,
        Type type) {

        return Optional.ofNullable(
            get(componentName, scope, scopeId, key, type));
    }

    @Override
    public <T> T get(
        String componentName, Scope scope, String scopeId, String key,
        Type type) {

        return get(componentName, scope, scopeId, key, type);
    }

    @Override
    public <T> Map<String, T> getAll(String componentName, Scope scope, String scopeId, Type type) {
        return getAll(componentName, scope, scopeId, type);
    }

    @Override
    public void put(
        String componentName, Scope scope, String scopeId, String key,
        Type type, Object value) {

        loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/save/{componentName}/{scope}/{scopeId}/{key}/{type}")
                .build(componentName, scope, scopeId, key, type),
            value);
    }

    @Override
    public void delete(String componentName, Scope scope, String scopeId, String key, Type type) {
        loadBalancedRestClient.delete(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/delete/{componentName}/{scope}/{scopeId}/{key}/{type}")
                .build(componentName, scope, scopeId, key, type));
    }

    private <T> T get(
        String componentName, Scope scope, String scopeId, String key, Type type,
        ParameterizedTypeReference<T> responseTypeRef) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(DATA_STORAGE_SERVICE
                    + "/fetch-value/{componentName}/{scope}/{scopeId}/{key}/{type}")
                .build(componentName, scope, scopeId, key, type),
            responseTypeRef);
    }
}
