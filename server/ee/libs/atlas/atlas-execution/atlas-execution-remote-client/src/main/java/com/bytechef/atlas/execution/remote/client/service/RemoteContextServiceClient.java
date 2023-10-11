
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteContextServiceClient implements ContextService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String INTERNAL_CONTEXT_SERVICE = "/remote/context-service";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteContextServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public FileEntry peek(long stackId, Classname classname) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(INTERNAL_CONTEXT_SERVICE + "/peek/{stackId}/{classname}")
                .build(stackId, classname),
            FileEntry.class);
    }

    @Override
    public FileEntry peek(long stackId, int subStackId, Classname classname) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(INTERNAL_CONTEXT_SERVICE + "/peek/{stackId}/{subStackId}/{classname}")
                .build(stackId, subStackId, classname),
            FileEntry.class);
    }

    @Override
    public void push(long stackId, Classname classname, FileEntry value) {
        loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(INTERNAL_CONTEXT_SERVICE + "/push/{stackId}/{classname}")
                .build(stackId, classname),
            value, FileEntry.class);
    }

    @Override
    public void push(long stackId, int subStackId, Classname classname, FileEntry value) {
        loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(INTERNAL_CONTEXT_SERVICE + "/push/{stackId}/{subStackId}/{classname}")
                .build(stackId, classname),
            value, new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
