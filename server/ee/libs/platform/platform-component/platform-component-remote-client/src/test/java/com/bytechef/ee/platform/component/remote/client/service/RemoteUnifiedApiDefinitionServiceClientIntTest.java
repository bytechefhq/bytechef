/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.webhook.WebhookComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler;
import com.bytechef.platform.component.domain.ComponentDefinition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ComponentRemoteClientIntTestConfiguration.class, properties = "spring.quartz.auto-startup=false")
class RemoteUnifiedApiDefinitionServiceClientIntTest {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ComponentWorkerFactory componentWorkerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<ComponentWorker> componentWorkers = new ArrayList<>();
    private ComponentWorker firstComponentWorker;
    private ComponentWorker secondComponentWorker;

    @BeforeEach
    void beforeEach() throws IOException {
        firstComponentWorker = componentWorkerFactory.start("worker-1");
        secondComponentWorker = componentWorkerFactory.start(
            "worker-2",
            ComponentRemoteClientIntTestConfiguration.componentDefinitionRegistry(
                applicationProperties,
                List.of(new GrpcTestComponentHandler(2, "gRPC Test v2"), new WebhookComponentHandler())));

        componentWorkers.add(firstComponentWorker);
        componentWorkers.add(secondComponentWorker);
    }

    @AfterEach
    void afterEach() {
        componentWorkers.forEach(ComponentWorker::close);
    }

    @Test
    void testGetUnifiedApiComponentDefinitionsAreMergedAcrossWorkers() {
        assertThat(createClient().getUnifiedApiComponentDefinitions(UnifiedApiCategory.CRM))
            .extracting(ComponentDefinition::getName, ComponentDefinition::getVersion)
            .containsExactlyInAnyOrder(tuple(COMPONENT_NAME, 1), tuple(COMPONENT_NAME, 2));
    }

    @Test
    void testGetUnifiedApiComponentDefinitionsWithoutCategory() {
        assertThat(createClient().getUnifiedApiComponentDefinitions(null))
            .extracting(ComponentDefinition::getName)
            .containsOnly(COMPONENT_NAME);
    }

    @Test
    void testGetUnifiedApiComponentDefinitionsSkipsAnUnreachableWorker() {
        RemoteUnifiedApiDefinitionServiceClient remoteUnifiedApiDefinitionServiceClient = createClient();

        secondComponentWorker.close();

        assertThat(remoteUnifiedApiDefinitionServiceClient.getUnifiedApiComponentDefinitions(UnifiedApiCategory.CRM))
            .extracting(ComponentDefinition::getName, ComponentDefinition::getVersion)
            .containsExactly(tuple(COMPONENT_NAME, 1));
    }

    private RemoteUnifiedApiDefinitionServiceClient createClient() {
        return new RemoteUnifiedApiDefinitionServiceClient(
            ComponentWorker.grpcChannels(firstComponentWorker, secondComponentWorker),
            ComponentWorker.discoveryClient(firstComponentWorker, secondComponentWorker), objectMapper);
    }
}
