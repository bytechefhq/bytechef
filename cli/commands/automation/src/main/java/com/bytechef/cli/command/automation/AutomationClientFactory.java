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

package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.api.ProjectCodeWorkflowApi;
import com.bytechef.cli.client.automation.api.ProjectGitApi;
import com.bytechef.cli.client.automationapiplatform.api.ApiCollectionApi;
import com.bytechef.cli.client.automationapiplatform.api.ApiCollectionEndpointApi;
import com.bytechef.cli.client.automationapp.api.AppApi;
import com.bytechef.cli.client.automationapp.api.AppApiCollectionBindingApi;
import com.bytechef.cli.client.automationapp.api.AppWorkflowBindingApi;
import com.bytechef.cli.client.automationapp.api.WorkspaceConnectionApi;
import com.bytechef.cli.client.automationdatatable.api.DataTableApi;
import com.bytechef.cli.client.automationdatatable.api.DataTableRowApi;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.http.AuthInterceptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

/**
 * Builds automation public API clients configured with the resolved host and auth headers, and maps client
 * {@link ApiException}s onto {@link CliException}s carrying the right exit code.
 *
 * @author Ivica Cardic
 */
final class AutomationClientFactory {

    static final String MANAGE_APPS_FORBIDDEN_HINT = "Managing apps needs the workspace admin role in DEVELOPMENT.";
    static final String BIND_CONNECTION_FORBIDDEN_HINT =
        "Binding a connection needs the workspace admin role in DEVELOPMENT and permission to use the connection.";

    private static final String NEUTRAL_FORBIDDEN_HINT =
        "Your role in DEVELOPMENT does not allow this; ask a workspace admin.";

    private static final String AUTOMATION_API_PATH = "/api/automation/v1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AutomationClientFactory() {
    }

    static ApiClient apiClient(CliConfig config) {
        ApiClient apiClient = new ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, AUTOMATION_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static ProjectCodeWorkflowApi projectCodeWorkflowApi(CliConfig config) {
        return new ProjectCodeWorkflowApi(apiClient(config));
    }

    static ProjectGitApi projectGitApi(CliConfig config) {
        return new ProjectGitApi(apiClient(config));
    }

    /**
     * The generated app client's {@code ApiClient} is a distinct type per generated package.
     */
    static com.bytechef.cli.client.automationapp.ApiClient appApiClient(CliConfig config) {
        com.bytechef.cli.client.automationapp.ApiClient apiClient =
            new com.bytechef.cli.client.automationapp.ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, AUTOMATION_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static AppApi appApi(CliConfig config) {
        return new AppApi(appApiClient(config));
    }

    static AppApiCollectionBindingApi appApiCollectionBindingApi(CliConfig config) {
        return new AppApiCollectionBindingApi(appApiClient(config));
    }

    static AppWorkflowBindingApi appWorkflowBindingApi(CliConfig config) {
        return new AppWorkflowBindingApi(appApiClient(config));
    }

    static WorkspaceConnectionApi workspaceConnectionApi(CliConfig config) {
        return new WorkspaceConnectionApi(appApiClient(config));
    }

    /**
     * The generated API Platform client's {@code ApiClient} is a distinct type per generated package, like the app one.
     */
    static com.bytechef.cli.client.automationapiplatform.ApiClient apiPlatformApiClient(CliConfig config) {
        com.bytechef.cli.client.automationapiplatform.ApiClient apiClient =
            new com.bytechef.cli.client.automationapiplatform.ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, AUTOMATION_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static ApiCollectionApi apiCollectionApi(CliConfig config) {
        return new ApiCollectionApi(apiPlatformApiClient(config));
    }

    static ApiCollectionEndpointApi apiCollectionEndpointApi(CliConfig config) {
        return new ApiCollectionEndpointApi(apiPlatformApiClient(config));
    }

    static com.bytechef.cli.client.automationdatatable.ApiClient dataTableApiClient(CliConfig config) {
        com.bytechef.cli.client.automationdatatable.ApiClient apiClient =
            new com.bytechef.cli.client.automationdatatable.ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, AUTOMATION_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static DataTableApi dataTableApi(CliConfig config) {
        return new DataTableApi(dataTableApiClient(config));
    }

    static DataTableRowApi dataTableRowApi(CliConfig config) {
        return new DataTableRowApi(dataTableApiClient(config));
    }

    static CliException toCliException(ApiException exception) {
        return toCliException(exception.getCode());
    }

    /**
     * Maps an app client failure onto the CLI's exit codes, carrying the server's {@code {code, message}} error body
     * and the next step, so a coding agent can correct itself. Status 0 is the generated client's code for an I/O
     * failure, which includes a 2xx reply it could not read.
     */
    static CliException toCliException(com.bytechef.cli.client.automationapp.ApiException exception) {
        return toCliException(exception, NEUTRAL_FORBIDDEN_HINT);
    }

    /**
     * Like {@link #toCliException(com.bytechef.cli.client.automationapp.ApiException)}, with the command's own remedy
     * for HTTP 403.
     */
    static CliException toCliException(
        com.bytechef.cli.client.automationapp.ApiException exception, String forbiddenHint) {

        return toCliException(
            exception.getCode(), exception.getMessage(), exception.getResponseBody(), forbiddenHint);
    }

    /**
     * Maps an API Platform client failure like an app client one, carrying the server's {@code {code, message}} error
     * body.
     */
    static CliException toCliException(com.bytechef.cli.client.automationapiplatform.ApiException exception) {
        return toCliException(
            exception.getCode(), exception.getMessage(), exception.getResponseBody(), NEUTRAL_FORBIDDEN_HINT);
    }

    static CliException toCliException(com.bytechef.cli.client.automationdatatable.ApiException exception) {
        return toCliException(exception.getCode());
    }

    private static CliException toCliException(
        int status, String exceptionMessage, String responseBody, String forbiddenHint) {

        if (status == 0) {
            return new CliException(
                1, "Cannot reach ByteChef, or the host is not a ByteChef server: "
                    + Objects.requireNonNullElse(exceptionMessage, "no response") + ".");
        }

        String detail = appErrorDetail(responseBody);

        if (status == 401) {
            return new CliException(
                2, "Authentication failed (HTTP 401)" + detail + ". Use an automation API key created in DEVELOPMENT "
                    + "(bytechef configure --environment DEVELOPMENT --token <key>).");
        }

        if (status == 403) {
            return new CliException(2, "Not allowed (HTTP 403)" + detail + ". " + forbiddenHint);
        }

        if (status == 404) {
            return new CliException(3, "Not found (HTTP 404)" + detail + ".");
        }

        return new CliException(1, "Request failed (HTTP " + status + ")" + detail + ".");
    }

    private static String appErrorDetail(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        JsonNode errorNode;

        try {
            errorNode = OBJECT_MAPPER.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            return "";
        }

        JsonNode codeNode = errorNode.path("code");
        JsonNode messageNode = errorNode.path("message");

        String code = codeNode.asText("");
        String message = messageNode.asText("");

        while (message.endsWith(".")) {
            message = message.substring(0, message.length() - 1);
        }

        if (code.isEmpty() && message.isEmpty()) {
            return "";
        }

        if (code.isEmpty()) {
            return ": " + message;
        }

        if (message.isEmpty()) {
            return ": " + code;
        }

        return ": " + code + ": " + message;
    }

    static CliException toCliException(int status) {
        if (status == 401 || status == 403) {
            return new CliException(2, "Authentication failed (HTTP " + status + ").");
        }

        if (status == 404) {
            return new CliException(3, "Not found (HTTP 404).");
        }

        return new CliException(1, "Request failed (HTTP " + status + ").");
    }
}
