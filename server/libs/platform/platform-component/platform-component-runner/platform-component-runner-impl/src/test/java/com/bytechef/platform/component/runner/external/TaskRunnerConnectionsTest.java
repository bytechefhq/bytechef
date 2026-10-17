/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskRunnerConnectionsTest {

    @Test
    void testEachConnectionNameMapsToItsParameters() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), enabled());

        assertThat(taskRunnerConnections.connections())
            .isEqualTo(Map.of("slack", Map.of("token", "xoxb-123456")));
    }

    @Test
    void testNamesAndParametersKeepTheirExactSpelling() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("github-bot", connection("github", Map.of("access.token", "gho_abcdef")))), enabled());

        assertThat(taskRunnerConnections.connections())
            .isEqualTo(Map.of("github-bot", Map.of("access.token", "gho_abcdef")));
    }

    @Test
    void testNonStringAndNullValuesAreKept() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("port", 5432);
        parameters.put("options", Map.of("ssl", true));
        parameters.put("missing", null);

        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("db", connection("postgresql", parameters))), enabled());

        assertThat(taskRunnerConnections.connections()
            .get("db")).isEqualTo(parameters);
    }

    @Test
    void testTheParametersAreTheOnesTheGraalVmRunnerGets() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of(
                "api", new ComponentConnection(
                    "httpClient", 1, 1L, Map.of("token", "bearer-abcdef"), AuthorizationType.BEARER_TOKEN))),
            enabled());

        assertThat(taskRunnerConnections.connections())
            .isEqualTo(Map.of("api", Map.of("token", "bearer-abcdef")));
    }

    @Test
    void testTheTaskRunnerConnectionIsNeverDelivered() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("taskRunner", connection("aws", Map.of("secretAccessKey", "aws-secret-key")))), enabled());

        assertThat(taskRunnerConnections.connections()).isEmpty();
        assertThat(taskRunnerConnections.secretMasker()
            .mask("aws-secret-key")).isEqualTo("aws-secret-key");
    }

    @Test
    void testValuesAndTheJsonFormsAreMasked() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), enabled());

        SecretMasker secretMasker = taskRunnerConnections.secretMasker();

        assertThat(secretMasker.mask("token xoxb-123456")).isEqualTo("token ******");
        assertThat(secretMasker.mask("json {\"token\":\"xoxb-123456\"}")).isEqualTo("json ******");
        assertThat(secretMasker.mask("file {\"slack\":{\"token\":\"xoxb-123456\"}}")).isEqualTo("file ******");
    }

    @Test
    void testAShortValueIsMaskedInsideItsConnectionsJsonForm() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("slack", connection("slack", Map.of("pin", "1234")))), enabled());

        assertThat(taskRunnerConnections.secretMasker()
            .mask("json {\"pin\":\"1234\"}")).isEqualTo("json ******");
    }

    @Test
    void testNestedStringValuesAreMasked() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("gcp", connection("google", Map.of("serviceAccount", Map.of("privateKey", "pk-abcdef"))))),
            enabled());

        assertThat(taskRunnerConnections.secretMasker()
            .mask("key pk-abcdef")).isEqualTo("key ******");
    }

    @Test
    void testShortValuesAndWellKnownNonSecretNamesAreNotMasked() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of(
                "api", new ComponentConnection(
                    "httpClient", 1, 1L,
                    Map.of("baseUrl", "https://api.example.com", "region", "us-east-1", "token", "bearer-abcdef"),
                    AuthorizationType.BEARER_TOKEN))),
            enabled());

        assertThat(taskRunnerConnections.secretMasker()
            .mask("BEARER_TOKEN us-east-1 https://api.example.com bearer-abcdef"))
                .isEqualTo("BEARER_TOKEN us-east-1 https://api.example.com ******");
    }

    @Test
    void testTheSwitchOffDeliversAndMasksNothing() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), new ApplicationProperties());

        assertThat(taskRunnerConnections.enabled()).isFalse();
        assertThat(taskRunnerConnections.connections()).isEmpty();
        assertThat(taskRunnerConnections.secretMasker()
            .mask("xoxb-123456")).isEqualTo("xoxb-123456");
    }

    @Test
    void testTheSwitchOnIsEnabledEvenWithoutConnections() {
        TaskRunnerConnections taskRunnerConnections = TaskRunnerConnections.of(request(Map.of()), enabled());

        assertThat(taskRunnerConnections.enabled()).isTrue();
        assertThat(taskRunnerConnections.connections()).isEmpty();
    }

    private static ComponentConnection connection(String componentName, Map<String, ?> parameters) {
        return new ComponentConnection(componentName, 1, 1L, parameters, null);
    }

    private static ApplicationProperties enabled() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .setInjectConnections(true);

        return applicationProperties;
    }

    private static TaskRunnerRequest request(Map<String, ComponentConnection> connections) {
        return new TaskRunnerRequest(
            "shell", null, List.of("true"), Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), null, connections, mock(ActionContext.class));
    }
}
