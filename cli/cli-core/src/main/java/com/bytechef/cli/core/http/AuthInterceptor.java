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

package com.bytechef.cli.core.http;

import com.bytechef.cli.core.config.CliConfig;
import java.net.http.HttpRequest;
import java.util.function.Consumer;

/**
 * Adds the public API authentication and environment headers to every outgoing request.
 *
 * @author Ivica Cardic
 */
public class AuthInterceptor implements Consumer<HttpRequest.Builder> {

    private final CliConfig config;

    public AuthInterceptor(CliConfig config) {
        this.config = config;
    }

    @Override
    public void accept(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + config.token());
        builder.header(
            "X-Environment", config.environment()
                .name());
    }

    public static String baseUri(CliConfig config, String apiPath) {
        return config.host() + apiPath;
    }
}
