/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionEndpointServiceTest {

    private final ApiEndpointRepository apiEndpointRepository = mock(ApiEndpointRepository.class);

    private ApiCollectionEndpointService apiCollectionEndpointService;

    @BeforeEach
    void setUp() {
        apiCollectionEndpointService = new ApiCollectionEndpointServiceImpl(apiEndpointRepository);

        ApiCollectionEndpoint persistedApiCollectionEndpoint = new ApiCollectionEndpoint();

        persistedApiCollectionEndpoint.setId(501L);
        persistedApiCollectionEndpoint.setApiCollectionId(100L);
        persistedApiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
        persistedApiCollectionEndpoint.setName("getOrders");
        persistedApiCollectionEndpoint.setPath("orders");
        persistedApiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

        when(apiEndpointRepository.findById(501L)).thenReturn(Optional.of(persistedApiCollectionEndpoint));
        when(apiEndpointRepository.save(any(ApiCollectionEndpoint.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * The persisted pointer must survive an update whose argument carries none —
     * {@code ApiCollectionEndpointDTO#toApiCollectionEndpoint()} never maps it, so blanking it here would orphan the
     * endpoint from its workflow on every ordinary save.
     */
    @Test
    void testUpdateKeepsPersistedProjectDeploymentWorkflowIdWhenTheArgumentCarriesNone() {
        ApiCollectionEndpoint apiCollectionEndpoint = endpointWithoutPointer();

        ApiCollectionEndpoint updatedApiCollectionEndpoint = apiCollectionEndpointService.update(apiCollectionEndpoint);

        assertThat(updatedApiCollectionEndpoint.getProjectDeploymentWorkflowId()).isEqualTo(901L);
        assertThat(updatedApiCollectionEndpoint.getName()).isEqualTo("listOrders");
    }

    @Test
    void testUpdateCopiesProjectDeploymentWorkflowIdWhenTheArgumentCarriesOne() {
        ApiCollectionEndpoint apiCollectionEndpoint = endpointWithoutPointer();

        apiCollectionEndpoint.setProjectDeploymentWorkflowId(902L);

        ApiCollectionEndpoint updatedApiCollectionEndpoint = apiCollectionEndpointService.update(apiCollectionEndpoint);

        assertThat(updatedApiCollectionEndpoint.getProjectDeploymentWorkflowId()).isEqualTo(902L);
    }

    private static ApiCollectionEndpoint endpointWithoutPointer() {
        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setId(501L);
        apiCollectionEndpoint.setApiCollectionId(100L);
        apiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
        apiCollectionEndpoint.setName("listOrders");
        apiCollectionEndpoint.setPath("orders");

        return apiCollectionEndpoint;
    }
}
