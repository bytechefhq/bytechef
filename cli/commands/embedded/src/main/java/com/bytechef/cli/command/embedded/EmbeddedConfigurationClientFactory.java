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

package com.bytechef.cli.command.embedded;

import com.bytechef.cli.client.embeddedconfiguration.ApiClient;
import com.bytechef.cli.client.embeddedconfiguration.ApiException;
import com.bytechef.cli.client.embeddedconfiguration.api.AutomationProjectCodeWorkflowApi;
import com.bytechef.cli.client.embeddedconfiguration.api.AutomationWorkflowProjectApi;
import com.bytechef.cli.client.embeddedconfiguration.api.ConnectedUserApi;
import com.bytechef.cli.client.embeddedconfiguration.api.ConnectedUserProjectWorkflowApi;
import com.bytechef.cli.client.embeddedconfiguration.api.ConnectionApi;
import com.bytechef.cli.client.embeddedconfiguration.api.IntegrationApi;
import com.bytechef.cli.client.embeddedconfiguration.api.IntegrationInstanceApi;
import com.bytechef.cli.client.embeddedconfiguration.api.IntegrationInstanceWorkflowApi;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.http.AuthInterceptor;

/**
 * Builds embedded-configuration public API clients and maps their {@link ApiException}s to exit codes.
 *
 * @author Ivica Cardic
 */
final class EmbeddedConfigurationClientFactory {

    private EmbeddedConfigurationClientFactory() {
    }

    static ApiClient apiClient(CliConfig config) {
        ApiClient apiClient = new ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, EmbeddedSupport.EMBEDDED_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static IntegrationApi integrationApi(CliConfig config) {
        return new IntegrationApi(apiClient(config));
    }

    static IntegrationInstanceApi integrationInstanceApi(CliConfig config) {
        return new IntegrationInstanceApi(apiClient(config));
    }

    static IntegrationInstanceWorkflowApi integrationInstanceWorkflowApi(CliConfig config) {
        return new IntegrationInstanceWorkflowApi(apiClient(config));
    }

    static AutomationWorkflowProjectApi automationWorkflowProjectApi(CliConfig config) {
        return new AutomationWorkflowProjectApi(apiClient(config));
    }

    static ConnectedUserProjectWorkflowApi connectedUserProjectWorkflowApi(CliConfig config) {
        return new ConnectedUserProjectWorkflowApi(apiClient(config));
    }

    static ConnectionApi connectionApi(CliConfig config) {
        return new ConnectionApi(apiClient(config));
    }

    static ConnectedUserApi connectedUserApi(CliConfig config) {
        return new ConnectedUserApi(apiClient(config));
    }

    static AutomationProjectCodeWorkflowApi automationProjectCodeWorkflowApi(CliConfig config) {
        return new AutomationProjectCodeWorkflowApi(apiClient(config));
    }

    static CliException toCliException(ApiException exception) {
        int status = exception.getCode();

        if (status == 401 || status == 403) {
            return new CliException(2, "Authentication failed (HTTP " + status + ").");
        }

        if (status == 404) {
            return new CliException(3, "Not found (HTTP 404).");
        }

        return new CliException(1, "Request failed (HTTP " + status + ").");
    }
}
