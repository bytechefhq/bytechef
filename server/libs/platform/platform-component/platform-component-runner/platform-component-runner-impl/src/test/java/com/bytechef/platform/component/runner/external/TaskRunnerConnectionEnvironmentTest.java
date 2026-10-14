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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskRunnerConnectionEnvironmentTest {

    @Test
    void testEachConnectionGetsAJsonVariableAndOneVariablePerParameter() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), enabled());

        assertThat(environment.variables())
            .containsEntry("BYTECHEF_CONNECTION_SLACK", "{\"token\":\"xoxb-123456\"}")
            .containsEntry("BYTECHEF_CONNECTION_SLACK_TOKEN", "xoxb-123456");
    }

    @Test
    void testKeysAndParameterNamesAreSanitised() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("github-bot", connection("github", Map.of("access.token", "gho_abcdef")))), enabled());

        assertThat(environment.variables())
            .containsKey("BYTECHEF_CONNECTION_GITHUB_BOT")
            .containsEntry("BYTECHEF_CONNECTION_GITHUB_BOT_ACCESS_TOKEN", "gho_abcdef");
    }

    @Test
    void testCamelCaseNamesAreSplitIntoWords() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("awsProd", connection("aws", Map.of("accessKeyId", "AKIAEXAMPLE1")))), enabled());

        assertThat(environment.variables())
            .containsKey("BYTECHEF_CONNECTION_AWS_PROD")
            .containsEntry("BYTECHEF_CONNECTION_AWS_PROD_ACCESS_KEY_ID", "AKIAEXAMPLE1");
    }

    @Test
    void testNonStringValuesAreJsonAndNullsAreSkipped() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("port", 5432);
        parameters.put("options", Map.of("ssl", true));
        parameters.put("missing", null);

        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("db", connection("postgresql", parameters))), enabled());

        assertThat(environment.variables())
            .containsEntry("BYTECHEF_CONNECTION_DB_PORT", "5432")
            .containsEntry("BYTECHEF_CONNECTION_DB_OPTIONS", "{\"ssl\":true}")
            .doesNotContainKey("BYTECHEF_CONNECTION_DB_MISSING");
    }

    @Test
    void testTheAuthorizationTypeIsExportedByName() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of(
                "api", new ComponentConnection(
                    "httpClient", 1, 1L, Map.of("token", "bearer-abcdef"), AuthorizationType.BEARER_TOKEN))),
            enabled());

        assertThat(environment.variables()).containsEntry("BYTECHEF_CONNECTION_API_AUTHORIZATION_TYPE", "BEARER_TOKEN");
    }

    @Test
    void testTheTaskRunnerConnectionIsNeverInjected() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("taskRunner", connection("aws", Map.of("secretAccessKey", "aws-secret-key")))), enabled());

        assertThat(environment.variables()).isEmpty();
        assertThat(environment.secretMasker()
            .mask("aws-secret-key")).isEqualTo("aws-secret-key");
    }

    @Test
    void testAStringWithANulCharacterIsLeftOutOfTheFlatForm() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("odd", connection("custom", Map.of("value", "abc\u0000def")))), enabled());

        assertThat(environment.variables())
            .containsKey("BYTECHEF_CONNECTION_ODD")
            .doesNotContainKey("BYTECHEF_CONNECTION_ODD_VALUE");
    }

    @Test
    void testKeysThatSanitiseAlikeFailNamingBothAndNoValues() {
        Map<String, ComponentConnection> connections = new LinkedHashMap<>();

        connections.put("github-bot", connection("github", Map.of("token", "first-secret")));
        connections.put("github_bot", connection("github", Map.of("token", "second-secret")));

        assertThatThrownBy(() -> TaskRunnerConnectionEnvironment.of(request(connections), enabled()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("connection 'github-bot'")
            .hasMessageContaining("connection 'github_bot'")
            .hasMessageContaining("BYTECHEF_CONNECTION_GITHUB_BOT")
            .hasMessageNotContaining("first-secret")
            .hasMessageNotContaining("second-secret");
    }

    @Test
    void testAParameterAndAnotherConnectionThatSanitiseAlikeFailNamingBoth() {
        Map<String, ComponentConnection> connections = new LinkedHashMap<>();

        connections.put("a", connection("a", Map.of("b", "first-secret")));
        connections.put("a_b", connection("a", Map.of("token", "second-secret")));

        assertThatThrownBy(() -> TaskRunnerConnectionEnvironment.of(request(connections), enabled()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("parameter 'b' of connection 'a'")
            .hasMessageContaining("connection 'a_b'")
            .hasMessageContaining("BYTECHEF_CONNECTION_A_B")
            .hasMessageNotContaining("first-secret")
            .hasMessageNotContaining("second-secret");
    }

    @Test
    void testValuesAndTheJsonFormAreMasked() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), enabled());

        SecretMasker secretMasker = environment.secretMasker();

        assertThat(secretMasker.mask("token xoxb-123456")).isEqualTo("token ******");
        assertThat(secretMasker.mask("json {\"token\":\"xoxb-123456\"}")).isEqualTo("json ******");
    }

    @Test
    void testNestedStringValuesAreMasked() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("gcp", connection("google", Map.of("serviceAccount", Map.of("privateKey", "pk-abcdef"))))),
            enabled());

        assertThat(environment.secretMasker()
            .mask("key pk-abcdef")).isEqualTo("key ******");
    }

    @Test
    void testShortValuesAndWellKnownNonSecretNamesAreNotMasked() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of(
                "api", new ComponentConnection(
                    "httpClient", 1, 1L,
                    Map.of("baseUrl", "https://api.example.com", "region", "us-east-1", "token", "bearer-abcdef"),
                    AuthorizationType.BEARER_TOKEN))),
            enabled());

        assertThat(environment.secretMasker()
            .mask("BEARER_TOKEN us-east-1 https://api.example.com bearer-abcdef"))
                .isEqualTo("BEARER_TOKEN us-east-1 https://api.example.com ******");
    }

    @Test
    void testTheSwitchOffInjectsAndMasksNothing() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), new ApplicationProperties());

        assertThat(environment.variables()).isEmpty();
        assertThat(environment.secretMasker()
            .mask("xoxb-123456")).isEqualTo("xoxb-123456");
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
