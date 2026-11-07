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

package com.bytechef.component.elevenlabs.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.voice.VoiceAgentContext;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.test.voice.FakeProviderWebSocket;
import com.bytechef.test.voice.JsonSupport;
import com.bytechef.test.voice.RecordingWebSocketEmitter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ElevenLabsVoiceAgentSignedUrlTest {

    @Test
    void testMissingSignedUrlSurfacesAClearError() throws Exception {
        Throwable error = startWithSignedUrlResponse(Map.of());

        assertThat(error).isInstanceOf(IllegalStateException.class)
            .hasMessage("ElevenLabs did not return a signed_url for agent agent-1");
    }

    @Test
    void testBlankSignedUrlSurfacesAClearError() throws Exception {
        Throwable error = startWithSignedUrlResponse(Map.of("signed_url", "   "));

        assertThat(error).isInstanceOf(IllegalStateException.class)
            .hasMessage("ElevenLabs did not return a signed_url for agent agent-1");
    }

    private Throwable startWithSignedUrlResponse(Map<String, Object> signedUrlResponseBody) throws Exception {
        RecordingWebSocketEmitter emitter = new RecordingWebSocketEmitter();
        FakeProviderWebSocket.Connector connector = new FakeProviderWebSocket.Connector();

        ActionContext actionContext = Mockito.mock(ActionContext.class);
        Context.Json json = JsonSupport.create();

        Mockito.when(actionContext.json(Mockito.any()))
            .thenAnswer(invocation -> {
                Context.ContextFunction<Context.Json, Object> jsonFunction = invocation.getArgument(0);

                return jsonFunction.apply(json);
            });

        Http.Response response = Mockito.mock(Http.Response.class);

        Mockito.when(response.getBody(Mockito.<TypeReference<Map<String, Object>>>any()))
            .thenReturn(signedUrlResponseBody);

        Http.Executor executor = Mockito.mock(Http.Executor.class, Mockito.RETURNS_SELF);

        Mockito.when(executor.execute())
            .thenReturn(response);

        Http http = Mockito.mock(Http.class);

        Mockito.when(http.get(Mockito.any()))
            .thenReturn(executor);

        Mockito.when(actionContext.http(Mockito.any()))
            .thenAnswer(invocation -> {
                Context.ContextFunction<Http, Object> httpFunction = invocation.getArgument(0);

                return httpFunction.apply(http);
            });

        WebSocketHandler handler = ElevenLabsVoiceAgent.of(connector)
            .getElement()
            .apply(
                ParametersFactory.create(Map.of("agentId", "agent-1")),
                ParametersFactory.create(Map.of("key", "xi-api-key", "value", "el-key")),
                new VoiceAgentContext(actionContext, VoiceAgentToolset.EMPTY));

        handler.handle(emitter);

        return emitter.error();
    }
}
