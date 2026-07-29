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

import com.bytechef.cli.client.embeddedexecution.ApiClient;
import com.bytechef.cli.client.embeddedexecution.ApiException;
import com.bytechef.cli.client.embeddedexecution.api.ActionApi;
import com.bytechef.cli.client.embeddedexecution.api.ToolApi;
import com.bytechef.cli.client.embeddedexecution.api.ToolInvocationApi;
import com.bytechef.cli.client.embeddedexecution.api.WorkflowExecutionApi;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.http.AuthInterceptor;

/**
 * Builds embedded-execution public API clients and maps their {@link ApiException}s to exit codes.
 *
 * @author Ivica Cardic
 */
final class EmbeddedExecutionClientFactory {

    private EmbeddedExecutionClientFactory() {
    }

    static ApiClient apiClient(CliConfig config) {
        ApiClient apiClient = new ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, EmbeddedSupport.EMBEDDED_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static WorkflowExecutionApi workflowExecutionApi(CliConfig config) {
        return new WorkflowExecutionApi(apiClient(config));
    }

    static ToolApi toolApi(CliConfig config) {
        return new ToolApi(apiClient(config));
    }

    static ActionApi actionApi(CliConfig config) {
        return new ActionApi(apiClient(config));
    }

    static ToolInvocationApi toolInvocationApi(CliConfig config) {
        return new ToolInvocationApi(apiClient(config));
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
