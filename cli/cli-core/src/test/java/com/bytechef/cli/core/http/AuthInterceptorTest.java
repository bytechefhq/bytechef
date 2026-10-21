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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AuthInterceptorTest {

    @Test
    void testAddsAuthorizationAndEnvironmentHeaders() {
        CliConfig config = new CliConfig("https://h", "btc_x", Environment.STAGING, 1L);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://h/api/automation/v1/ping"));

        new AuthInterceptor(config).accept(builder);

        HttpRequest request = builder.GET()
            .build();

        assertEquals(
            List.of("Bearer btc_x"), request.headers()
                .allValues("Authorization"));
        assertEquals(
            List.of("STAGING"), request.headers()
                .allValues("X-Environment"));
    }

    @Test
    void testBaseUriAppendsApiPath() {
        assertTrue(
            AuthInterceptor.baseUri(new CliConfig("https://h", "t", Environment.PRODUCTION, null), "/api/embedded/v1")
                .endsWith("/api/embedded/v1"));
    }
}
