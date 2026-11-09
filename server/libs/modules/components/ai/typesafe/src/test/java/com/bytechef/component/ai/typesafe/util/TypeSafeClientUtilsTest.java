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

package com.bytechef.component.ai.typesafe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.TypeSafeClient;

/**
 * @author Ivica Cardic
 */
class TypeSafeClientUtilsTest {

    @Test
    void testCreateTypeSafeClientSendsRequestsToTheBaseUrl() throws Exception {
        List<String> requestPaths = new CopyOnWriteArrayList<>();

        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        httpServer.createContext("/", exchange -> {
            requestPaths.add(exchange.getRequestURI()
                .getPath());

            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        httpServer.start();

        try {
            Parameters connectionParameters = MockParametersFactory.create(
                Map.of(
                    "token", "local-test", "baseUrl",
                    "http://localhost:%d".formatted(httpServer.getAddress()
                        .getPort())));

            TypeSafeClient typeSafeClient = TypeSafeClientUtils.createTypeSafeClient(
                MockParametersFactory.create(Map.of()), connectionParameters);

            assertThrows(RuntimeException.class, typeSafeClient::listModels);
            assertEquals(List.of("/v1/models"), requestPaths);
        } finally {
            httpServer.stop(0);
        }
    }
}
