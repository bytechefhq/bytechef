/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.Map;
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
class RemoteCodeEditorScriptInputFacadeClientIntTest {

    private static final String ROWS_JSON = """
        [{"name":"Ada","address":{"city":"Zagreb"}},{"name":"Grace","address":{"city":"Arlington"}}]
        """;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ComponentWorkerFactory componentWorkerFactory;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TempFileStorage tempFileStorage;

    private ComponentWorker componentWorker;
    private RemoteCodeEditorScriptInputFacadeClient remoteCodeEditorScriptInputFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        remoteCodeEditorScriptInputFacadeClient = new RemoteCodeEditorScriptInputFacadeClient(
            applicationProperties, ComponentWorker.grpcChannels(componentWorker),
            ComponentWorker.discoveryClient(componentWorker), objectMapper);
    }

    @AfterEach
    void afterEach() {
        componentWorker.close();

        connectionRepository.deleteAll();
    }

    @Test
    void testGetScriptInputParametersReadsTheFirstRecordOfTheSource() {
        assertThat(
            remoteCodeEditorScriptInputFacadeClient.getScriptInputParameters(
                "dataStream", 1, "jsonFile", 1, "reader", readerInputParameters(), null))
                    .isEqualTo(Map.of("item", Map.of("name", "Ada", "address", Map.of("city", "Zagreb"))));
    }

    @Test
    void testGetScriptInputParametersWithASourceConnection() {
        Connection connection = connectionService.create(
            Connection.builder()
                .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .componentName(COMPONENT_NAME)
                .connectionVersion(1)
                .name("gRPC test")
                .parameters(Map.of("region", "us"))
                .type(PlatformType.AUTOMATION)
                .build());

        assertThat(
            remoteCodeEditorScriptInputFacadeClient.getScriptInputParameters(
                "dataStream", 1, "jsonFile", 1, "reader", readerInputParameters(), connection.getId()))
                    .isEqualTo(Map.of("item", Map.of("name", "Ada", "address", Map.of("city", "Zagreb"))));
    }

    @Test
    void testGetScriptInputParametersOfAnUnknownSourceConnectionFailsWithItsMessage() {
        assertThatThrownBy(
            () -> remoteCodeEditorScriptInputFacadeClient.getScriptInputParameters(
                "dataStream", 1, "jsonFile", 1, "reader", readerInputParameters(), Long.MAX_VALUE))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("Connection does not exist for id=" + Long.MAX_VALUE);
    }

    @Test
    void testGetScriptInputParametersWithoutProviderReturnsEmptyMap() {
        assertThat(
            remoteCodeEditorScriptInputFacadeClient.getScriptInputParameters(
                COMPONENT_NAME, 1, "jsonFile", 1, "reader", readerInputParameters(), null))
                    .isEmpty();
    }

    private Map<String, Object> readerInputParameters() {
        FileEntry fileEntry = tempFileStorage.storeFileContent("rows.json", ROWS_JSON);

        return Map.of("fileType", "JSON", "fileEntry", objectMapper.convertValue(fileEntry, Map.class));
    }
}
