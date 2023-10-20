/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.handler.http.client;

import com.integri.atlas.engine.context.Context;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.http.client.authentication.HttpAuthenticationFactory;
import com.integri.atlas.task.handler.http.client.body.HttpBodyFactory;
import com.integri.atlas.task.handler.http.client.header.HttpHeadersFactory;
import com.integri.atlas.task.handler.http.client.params.QueryParamsFactory;
import com.integri.atlas.task.handler.http.client.response.HttpResponseHandler;
import com.integri.atlas.test.task.handler.BaseTaskIntTest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Matija Petanjek
 */
@SpringBootTest
public class HttpClientTaskHandlerIntTest extends BaseTaskIntTest {

    private static HttpServer httpServer;
    private static ExecutorService executorService;

    @BeforeAll
    public static void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 8081), 0);
        executorService = Executors.newFixedThreadPool(5);
        httpServer.setExecutor(executorService);
        httpServer.createContext("/test", new TestHttpHandler());
        httpServer.start();
    }

    @AfterAll
    public static void tearDown() {
        httpServer.stop(0);
        executorService.shutdownNow();
    }

    @Test
    public void testHttpClientTaskHandler() {
        Job job = startJob("samples/http-client.yaml", Collections.emptyMap());

        Context context = contextRepository.peek(job.getId());

        Assertions.assertTrue(true);
    }

    @Override
    protected Map<String, TaskHandler<?>> getTaskHandlerResolverMap() {
        return Map.of(
            "httpClient",
            new HttpClientTaskHandler(
                new HttpBodyFactory(fileStorageService, jsonHelper),
                new HttpAuthenticationFactory(),
                new HttpHeadersFactory(jsonHelper),
                new HttpResponseHandler(fileStorageService, jsonHelper),
                new QueryParamsFactory(jsonHelper)
            )
        );
    }
}
