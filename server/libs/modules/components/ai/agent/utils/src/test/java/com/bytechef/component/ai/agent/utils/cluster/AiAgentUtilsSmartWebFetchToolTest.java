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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class AiAgentUtilsSmartWebFetchToolTest {

    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();

    private HttpServer httpServer;
    private int port;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        port = httpServer.getAddress()
            .getPort();

        httpServer.createContext("/secret", httpExchange -> respond(httpExchange, 200, null, "internal secret"));
        httpServer.createContext("/public", httpExchange -> respond(httpExchange, 200, null, "public page"));
        httpServer.createContext(
            "/to-loopback-address",
            httpExchange -> respond(httpExchange, 302, "http://127.0.0.1:" + port + "/secret", ""));
        httpServer.createContext("/to-allowed-host", httpExchange -> respond(httpExchange, 302, "/public", ""));

        httpServer.start();
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);
    }

    @Test
    void testFetchOfAnAllowedHostFollowsItsRedirects() throws Exception {
        String result = fetch("http://localhost:" + port + "/to-allowed-host");

        assertThat(result).contains("public page");
        assertThat(requestedPaths).containsExactly("/to-allowed-host", "/public");
    }

    @Test
    void testFetchRefusesARedirectToAPrivateAddress() throws Exception {
        String result = fetch("http://localhost:" + port + "/to-loopback-address");

        assertThat(result).doesNotContain("internal secret");
        assertThat(requestedPaths).doesNotContain("/secret");
    }

    @Test
    void testFetchRefusesAPrivateAddress() throws Exception {
        String result = fetch("http://127.0.0.1:" + port + "/secret");

        assertThat(result).startsWith("Error:")
            .doesNotContain("internal secret");
        assertThat(requestedPaths).isEmpty();
    }

    private String fetch(String url) throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModelFunction modelFunction =
            (inputParameters, connectionParameters, responseFormatRequired) -> new EchoChatModel();

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(eq("openAi"), eq(1), eq("model")))
            .thenReturn(modelFunction);

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of("model", Map.of("name", "openAi_1", "type", "openAi/v1/model", "parameters", Map.of()))));

        MultipleConnectionsToolCallbackProviderFunction toolCallbackProviderFunction =
            new AiAgentUtilsSmartWebFetchTool(clusterElementDefinitionService,
                Set.of("localhost")).clusterElementDefinition.getElement();

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction.apply(
            MockParametersFactory.create(Map.of(AiAgentUtilsSmartWebFetchTool.DOMAIN_SAFETY_CHECK, false)),
            MockParametersFactory.create(Map.of()), extensions, Map.of(), mock(Context.class))
            .getToolCallbacks();

        return toolCallbacks[0].call(JsonUtils.write(Map.of("url", url, "prompt", "Return the page text")));
    }

    private void respond(HttpExchange httpExchange, int statusCode, String location, String body)
        throws IOException {

        requestedPaths.add(httpExchange.getRequestURI()
            .getPath());

        if (location != null) {
            httpExchange.getResponseHeaders()
                .add("Location", location);
        }

        httpExchange.getResponseHeaders()
            .add("Content-Type", "text/html; charset=utf-8");

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        httpExchange.sendResponseHeaders(statusCode, bytes.length == 0 ? -1 : bytes.length);

        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static final class EchoChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(prompt.getContents()))));
        }
    }
}
