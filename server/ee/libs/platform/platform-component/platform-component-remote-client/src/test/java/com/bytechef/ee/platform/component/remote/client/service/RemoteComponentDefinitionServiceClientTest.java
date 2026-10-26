/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class RemoteComponentDefinitionServiceClientTest {

    private final DefaultRestClient defaultRestClient = mock(DefaultRestClient.class);
    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);

    private final RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient =
        new RemoteComponentDefinitionServiceClient(defaultRestClient, discoveryClient, mock(ObjectMapper.class));

    @Test
    @SuppressWarnings("unchecked")
    void testGetComponentDefinitionsSendsIncludeAndPlatformType() {
        when(discoveryClient.getInstances("worker-app")).thenReturn(
            List.of(new DefaultServiceInstance(
                "worker-app:1", "worker-app", "worker-app", 8080, false,
                Map.of("components", "[{\"name\":\"slack\"}]"))));
        when(defaultRestClient.get(any(Function.class), any(ParameterizedTypeReference.class)))
            .thenReturn(List.of());

        remoteComponentDefinitionServiceClient.getComponentDefinitions(
            true, null, null, true, List.of("slack", "gmail"), PlatformType.AUTOMATION);

        ArgumentCaptor<Function<UriBuilder, URI>> uriFunctionArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        verify(defaultRestClient).get(uriFunctionArgumentCaptor.capture(), any(ParameterizedTypeReference.class));

        Function<UriBuilder, URI> uriFunction = uriFunctionArgumentCaptor.getValue();

        URI uri = uriFunction.apply(new DefaultUriBuilderFactory().builder());

        assertThat(uri.getQuery()).contains(
            "actionDefinitions=true", "triggerDefinitions=true", "include=slack", "include=gmail",
            "platformType=AUTOMATION");
    }
}
