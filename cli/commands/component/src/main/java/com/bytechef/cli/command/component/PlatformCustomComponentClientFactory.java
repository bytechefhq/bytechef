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

package com.bytechef.cli.command.component;

import com.bytechef.cli.client.platformcustomcomponent.ApiClient;
import com.bytechef.cli.client.platformcustomcomponent.ApiException;
import com.bytechef.cli.client.platformcustomcomponent.api.CustomComponentApi;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.http.AuthInterceptor;

/**
 * @author Ivica Cardic
 */
final class PlatformCustomComponentClientFactory {

    private PlatformCustomComponentClientFactory() {
    }

    static ApiClient apiClient(CliConfig config) {
        ApiClient apiClient = new ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, ComponentSupport.PLATFORM_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static CustomComponentApi customComponentApi(CliConfig config) {
        return new CustomComponentApi(apiClient(config));
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
