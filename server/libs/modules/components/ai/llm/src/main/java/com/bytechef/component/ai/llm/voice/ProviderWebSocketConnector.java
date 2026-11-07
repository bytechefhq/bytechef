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

package com.bytechef.component.ai.llm.voice;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ProviderWebSocketConnector {

    WebSocket connect(URI uri, Map<String, String> headers, WebSocket.Listener listener);

    static ProviderWebSocketConnector jdk() {
        return (uri, headers, listener) -> {
            HttpClient httpClient = SharedHttpClient.get();

            WebSocket.Builder builder = httpClient.newWebSocketBuilder();

            headers.forEach(builder::header);

            AtomicReference<SerializedWebSocket> serializedWebSocketHolder = new AtomicReference<>();
            SerializingWebSocketListener serializingWebSocketListener =
                new SerializingWebSocketListener(listener, serializedWebSocketHolder);

            WebSocket rawWebSocket = builder.buildAsync(uri, serializingWebSocketListener)
                .join();

            return serializingWebSocketListener.serialized(rawWebSocket);
        };
    }
}
