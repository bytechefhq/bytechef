/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import tools.jackson.core.type.TypeReference;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkerDiscoveryUtilsTest {

    @Test
    void testFetchServiceInstancePicksTheInstanceOfferingTheComponent() {
        ServiceInstance firstServiceInstance = serviceInstance("worker-1", "[{\"name\":\"airtable\"}]");
        ServiceInstance secondServiceInstance = serviceInstance("worker-2", "[{\"name\":\"slack\"}]");

        Optional<ServiceInstance> serviceInstance = WorkerDiscoveryUtils.fetchServiceInstance(
            List.of(firstServiceInstance, secondServiceInstance), "Slack");

        assertThat(serviceInstance).contains(secondServiceInstance);
        assertThat(WorkerDiscoveryUtils.fetchServiceInstance(List.of(firstServiceInstance), "slack")).isEmpty();
    }

    @Test
    void testComponentsMetadataIsParsedOncePerRegistration() {
        String componentsMetadata = "[{\"name\":\"github\"},{\"name\":\"gitlab\"}]";
        String changedComponentsMetadata = "[{\"name\":\"github\"},{\"name\":\"gitlab\"},{\"name\":\"jira\"}]";

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = mockStatic(JsonUtils.class, CALLS_REAL_METHODS)) {
            List<ServiceInstance> serviceInstances = List.of(serviceInstance("worker-1", componentsMetadata));

            for (int index = 0; index < 3; index++) {
                assertThat(WorkerDiscoveryUtils.fetchServiceInstance(serviceInstances, "gitlab")).isPresent();
                assertThat(WorkerDiscoveryUtils.filterServiceInstances(serviceInstances)).hasSize(1);
            }

            jsonUtilsMockedStatic.verify(
                () -> JsonUtils.read(eq(componentsMetadata), any(TypeReference.class)), times(1));

            List<ServiceInstance> changedServiceInstances = List.of(
                serviceInstance("worker-1", changedComponentsMetadata));

            assertThat(WorkerDiscoveryUtils.fetchServiceInstance(changedServiceInstances, "jira")).isPresent();
            assertThat(WorkerDiscoveryUtils.fetchServiceInstance(changedServiceInstances, "jira")).isPresent();

            jsonUtilsMockedStatic.verify(
                () -> JsonUtils.read(eq(changedComponentsMetadata), any(TypeReference.class)), times(1));
        }
    }

    private static ServiceInstance serviceInstance(String instanceId, String componentsMetadata) {
        return new DefaultServiceInstance(
            instanceId, "worker-app", "localhost", 8080, false, Map.of("components", componentsMetadata));
    }
}
