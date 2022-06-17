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

package com.bytechef.task.handler.httpclient.v1_0;

import com.bytechef.atlas.context.domain.Context;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.task.handler.httpclient.util.TestHTTPHandler;
import com.bytechef.task.handler.httpclient.v1_0.http.HTTPClientHelper;
import com.bytechef.test.support.task.BaseTaskIntTest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Matija Petanjek
 */
@SpringBootTest
public class HTTPClientTaskHandlerIntTest extends BaseTaskIntTest {

    private static HttpServer httpServer;
    private static ExecutorService executorService;

    @Autowired
    private HTTPClientHelper httpClientHelper;

    @BeforeAll
    public static void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 8081), 0);
        executorService = Executors.newFixedThreadPool(5);

        httpServer.setExecutor(executorService);
        httpServer.createContext("/test", new TestHTTPHandler());
        httpServer.start();
    }

    @AfterAll
    public static void tearDown() {
        httpServer.stop(0);
        executorService.shutdownNow();
    }

    @Test
    public void testHTTPClientDeleteTaskHandler() {
        Assertions.fail();
    }

    @Test
    public void testHTTPClientGetTaskHandler() {
        Job job = startJob("samples/v1_0/http-client-get.yaml", Collections.emptyMap());

        Context context = contextService.peek(job.getId());

        Assertions.fail();
    }

    @Test
    public void testHTTPClientHeadTaskHandler() {
        Assertions.fail();
    }

    @Test
    public void testHTTPClientPatchTaskHandler() {
        Assertions.fail();
    }

    @Test
    public void testHTTPClientPostTaskHandler() {
        Assertions.fail();
    }

    @Test
    public void testHTTPClientPutTaskHandler() {
        Assertions.fail();
    }
}
