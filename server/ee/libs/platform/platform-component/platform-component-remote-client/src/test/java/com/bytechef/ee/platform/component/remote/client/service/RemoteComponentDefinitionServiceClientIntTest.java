/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.component.webhook.WebhookComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionRemoteServiceGrpc;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.simple.InstanceProperties;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ComponentRemoteClientIntTestConfiguration.class, properties = "spring.quartz.auto-startup=false")
class RemoteComponentDefinitionServiceClientIntTest {

    private static final String GET_COMPONENT_DEFINITIONS = ComponentDefinitionRemoteServiceGrpc
        .getGetComponentDefinitionsMethod()
        .getFullMethodName();

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ComponentWorkerFactory componentWorkerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<ComponentWorker> componentWorkers = new ArrayList<>();
    private ComponentWorker firstComponentWorker;
    private ComponentWorker secondComponentWorker;
    private SimpleDiscoveryProperties simpleDiscoveryProperties;

    @BeforeEach
    void beforeEach() throws IOException {
        firstComponentWorker = startComponentWorker("worker-1");
        secondComponentWorker = startSecondComponentWorker("worker-2");

        simpleDiscoveryProperties = new SimpleDiscoveryProperties();
    }

    @AfterEach
    void afterEach() {
        componentWorkers.forEach(ComponentWorker::close);
        componentWorkers.clear();
    }

    @Test
    void testGetComponentDefinitionsMergesTheWorkersOncePerNameAndVersion() {
        List<ComponentDefinition> componentDefinitions = createClient(firstComponentWorker, secondComponentWorker)
            .getComponentDefinitions();

        assertThat(componentDefinitions)
            .extracting(ComponentDefinition::getName, ComponentDefinition::getVersion)
            .containsExactly(
                tuple("dataStream", 1), tuple(COMPONENT_NAME, 1), tuple(COMPONENT_NAME, 2), tuple("jsonFile", 1),
                tuple("manual", 1), tuple("missing", 1), tuple("schedule", 1), tuple("webhook", 1));
        assertThat(componentDefinitions)
            .filteredOn(componentDefinition -> componentDefinition.getVersion() == 2)
            .extracting(ComponentDefinition::getTitle)
            .containsExactly("gRPC Test v2");
    }

    @Test
    void testGetComponentDefinitionsAppliesTheServerFlagsAndTheCallerPlatformFilter() {
        List<String> triggerComponentNames = getComponentNames(
            firstComponentWorker.getComponentWorkerServices()
                .componentDefinitionService()
                .getComponentDefinitions(null, null, null, true, null, null));

        assertThat(triggerComponentNames).contains(COMPONENT_NAME, "schedule", "webhook")
            .doesNotContain("jsonFile");
        assertThat(
            createClient(firstComponentWorker).getComponentDefinitions(
                null, null, null, true, null, PlatformType.EMBEDDED))
                    .extracting(ComponentDefinition::getName)
                    .containsExactlyElementsOf(triggerComponentNames);
        assertThat(
            createClient(firstComponentWorker).getComponentDefinitions(
                null, null, null, true, null, PlatformType.AUTOMATION))
                    .extracting(ComponentDefinition::getName)
                    .containsExactlyElementsOf(
                        triggerComponentNames.stream()
                            .filter(componentName -> !"webhook".equals(componentName))
                            .toList());
    }

    @Test
    void testGetComponentDefinitionsWithIncludeKeepsIncludeOrder() {
        assertThat(
            createClient(firstComponentWorker).getComponentDefinitions(
                true, null, null, null, List.of("webhook", "jsonFile"), null))
                    .extracting(ComponentDefinition::getName)
                    .containsExactly("webhook", "jsonFile");
    }

    @Test
    void testGetComponentDefinitionsSkipsAnUnreachableWorkerAndDoesNotCacheTheResult() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker, secondComponentWorker);

        secondComponentWorker.close();

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions())
            .extracting(ComponentDefinition::getName, ComponentDefinition::getVersion)
            .doesNotContain(tuple(COMPONENT_NAME, 2))
            .contains(tuple(COMPONENT_NAME, 1), tuple("webhook", 1));

        remoteComponentDefinitionServiceClient.getComponentDefinitions();

        assertThat(firstComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(2);
    }

    @Test
    void testRepeatedListIsServedFromTheCacheWhileTheWorkersAreUnchanged() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker, secondComponentWorker);

        List<ComponentDefinition> componentDefinitions = remoteComponentDefinitionServiceClient
            .getComponentDefinitions();

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions())
            .extracting(ComponentDefinition::getName, ComponentDefinition::getVersion)
            .containsExactlyElementsOf(
                componentDefinitions.stream()
                    .map(componentDefinition -> tuple(componentDefinition.getName(), componentDefinition.getVersion()))
                    .toList());
        assertThat(firstComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(1);
        assertThat(secondComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(1);
    }

    @Test
    void testListIsFetchedAgainWhenAWorkerJoins() throws IOException {
        ComponentWorker thirdComponentWorker = startComponentWorker("worker-3");

        ComponentWorker.register(simpleDiscoveryProperties, firstComponentWorker, secondComponentWorker);

        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient =
            new RemoteComponentDefinitionServiceClient(
                ComponentWorker.grpcChannels(firstComponentWorker, secondComponentWorker, thirdComponentWorker),
                new SimpleDiscoveryClient(simpleDiscoveryProperties), objectMapper, List.of());

        remoteComponentDefinitionServiceClient.getComponentDefinitions();

        ComponentWorker.register(
            simpleDiscoveryProperties, firstComponentWorker, secondComponentWorker, thirdComponentWorker);

        remoteComponentDefinitionServiceClient.getComponentDefinitions();

        assertThat(firstComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(2);
        assertThat(thirdComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(1);
    }

    @Test
    void testListIsFetchedAgainWhenAWorkerOffersOtherComponents() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker, secondComponentWorker);

        remoteComponentDefinitionServiceClient.getComponentDefinitions();

        InstanceProperties secondInstanceProperties = secondComponentWorker.toInstanceProperties(
            "[{\"name\":\"" + COMPONENT_NAME + "\"}]");

        simpleDiscoveryProperties.setInstances(
            Map.of(
                ComponentWorker.WORKER_APP,
                List.of(
                    simpleDiscoveryProperties.getInstances()
                        .get(ComponentWorker.WORKER_APP)
                        .getFirst(),
                    secondInstanceProperties)));

        remoteComponentDefinitionServiceClient.getComponentDefinitions();

        assertThat(firstComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(2);
        assertThat(secondComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(2);
    }

    @Test
    void testListsOfDifferentRequestsAreCachedSeparately() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker);

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions())
            .extracting(ComponentDefinition::getName)
            .contains("jsonFile");
        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions(null, null, null, true, null, null))
            .extracting(ComponentDefinition::getName)
            .doesNotContain("jsonFile")
            .contains(COMPONENT_NAME, "webhook");
        assertThat(firstComponentWorker.getCallCount(GET_COMPONENT_DEFINITIONS)).isEqualTo(2);
    }

    @Test
    void testGetComponentDefinitionReturnsWhatTheWorkerServes() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker);
        ComponentDefinitionService componentDefinitionService = firstComponentWorker.getComponentWorkerServices()
            .componentDefinitionService();

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinition(COMPONENT_NAME, 1))
            .usingRecursiveComparison()
            .isEqualTo(componentDefinitionService.getComponentDefinition(COMPONENT_NAME, 1));
        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinition("webhook", null))
            .usingRecursiveComparison()
            .isEqualTo(componentDefinitionService.getComponentDefinition("webhook", null));
    }

    @Test
    void testGetComponentDefinitionWithoutVersionReturnsTheLatest() {
        assertThat(createClient(secondComponentWorker).getComponentDefinition(COMPONENT_NAME, null))
            .extracting(ComponentDefinition::getVersion, ComponentDefinition::getTitle)
            .containsExactly(2, "gRPC Test v2");
    }

    @Test
    void testFetchComponentDefinitionReturnsItOrEmpty() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker);

        assertThat(remoteComponentDefinitionServiceClient.fetchComponentDefinition(COMPONENT_NAME, 1))
            .get()
            .extracting(ComponentDefinition::getName)
            .isEqualTo(COMPONENT_NAME);
        assertThat(remoteComponentDefinitionServiceClient.fetchComponentDefinition(COMPONENT_NAME, 7)).isEmpty();
        assertThat(remoteComponentDefinitionServiceClient.fetchComponentDefinition("nope", null)).isEmpty();
    }

    @Test
    void testHasComponentDefinitionAsksTheWorkerWithoutFetchingTheDefinition() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker);

        assertThat(remoteComponentDefinitionServiceClient.hasComponentDefinition(COMPONENT_NAME, 1)).isTrue();
        assertThat(remoteComponentDefinitionServiceClient.hasComponentDefinition(COMPONENT_NAME, 7)).isFalse();
        assertThat(remoteComponentDefinitionServiceClient.hasComponentDefinition("nope", null)).isFalse();
        assertThat(
            firstComponentWorker.getCallCount(
                ComponentDefinitionRemoteServiceGrpc.getGetComponentDefinitionMethod()
                    .getFullMethodName())).isZero();
        assertThat(
            firstComponentWorker.getCallCount(
                ComponentDefinitionRemoteServiceGrpc.getHasComponentDefinitionMethod()
                    .getFullMethodName())).isEqualTo(2);
    }

    @Test
    void testComponentOnNoWorkerFailsNamingTheComponent() {
        assertThatThrownBy(() -> createClient(firstComponentWorker).getComponentDefinition("nope", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No worker instance offers component nope");
    }

    @Test
    void testSearchComponentDefinitionsAppliesTheCallerPlatformFilter() {
        RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient = createClient(
            firstComponentWorker);

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions("webhook", PlatformType.EMBEDDED))
            .extracting(ComponentDefinition::getName)
            .containsExactly("webhook");
        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions("webhook", PlatformType.AUTOMATION))
            .isEmpty();
    }

    @Test
    void testGetConnectionComponentDefinitionReturnsTheComponentOfThatConnectionVersion() {
        ComponentDefinition componentDefinition = createClient(firstComponentWorker)
            .getConnectionComponentDefinition(COMPONENT_NAME, 1);

        assertThat(componentDefinition.getName()).isEqualTo(COMPONENT_NAME);
        assertThat(componentDefinition.getConnection()).isNotNull();
    }

    @Test
    void testGetComponentDefinitionVersionsMergesTheWorkers() {
        assertThat(
            createClient(firstComponentWorker, secondComponentWorker).getComponentDefinitionVersions(COMPONENT_NAME))
                .extracting(ComponentDefinition::getVersion, ComponentDefinition::getTitle)
                .containsExactly(tuple(1, "gRPC Test"), tuple(2, "gRPC Test v2"));
    }

    private static List<String> getComponentNames(List<ComponentDefinition> componentDefinitions) {
        return componentDefinitions.stream()
            .map(ComponentDefinition::getName)
            .toList();
    }

    private RemoteComponentDefinitionServiceClient createClient(ComponentWorker... discoveredComponentWorkers) {
        ComponentWorker.register(simpleDiscoveryProperties, discoveredComponentWorkers);

        return new RemoteComponentDefinitionServiceClient(
            ComponentWorker.grpcChannels(discoveredComponentWorkers),
            new SimpleDiscoveryClient(simpleDiscoveryProperties), objectMapper,
            List.of(new NoWebhookForAutomationComponentDefinitionFilter(),
                new AllForEmbeddedComponentDefinitionFilter()));
    }

    private ComponentWorker startComponentWorker(String instanceId) throws IOException {
        ComponentWorker componentWorker = componentWorkerFactory.start(instanceId);

        componentWorkers.add(componentWorker);

        return componentWorker;
    }

    private ComponentWorker startSecondComponentWorker(String instanceId) throws IOException {
        ComponentWorker componentWorker = componentWorkerFactory.start(
            instanceId,
            ComponentRemoteClientIntTestConfiguration.componentDefinitionRegistry(
                applicationProperties,
                List.of(
                    new GrpcTestComponentHandler(1, "gRPC Test"), new GrpcTestComponentHandler(2, "gRPC Test v2"),
                    new WebhookComponentHandler())));

        componentWorkers.add(componentWorker);

        return componentWorker;
    }

    private static final class AllForEmbeddedComponentDefinitionFilter implements ComponentDefinitionFilter {

        @Override
        public boolean filter(ComponentDefinition componentDefinition) {
            return true;
        }

        @Override
        public boolean supports(PlatformType type) {
            return type == PlatformType.EMBEDDED;
        }
    }

    private static final class NoWebhookForAutomationComponentDefinitionFilter implements ComponentDefinitionFilter {

        @Override
        public boolean filter(ComponentDefinition componentDefinition) {
            return !"webhook".equals(componentDefinition.getName());
        }

        @Override
        public boolean supports(PlatformType type) {
            return type == PlatformType.AUTOMATION;
        }
    }
}
