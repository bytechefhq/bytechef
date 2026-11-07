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

package com.bytechef.platform.webhook.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

@SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"
})
class VoiceSessionRegistryTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-08T10:00:00Z"));
    private final VoiceSessionRegistry registry = new VoiceSessionRegistry(now::get);

    @Test
    void testRegisterThenGet() {
        WebSocketSession webSocketSession = webSocketSession("ws-1");

        registry.register("s-1", webSocketSession, "wf-exec-1");

        assertThat(registry.get("s-1"))
            .get()
            .satisfies(session -> {
                assertThat(WebSocketSessionDecorator.unwrap(session.webSocketSession())).isSameAs(webSocketSession);
                assertThat(session.workflowExecutionId()).isEqualTo("wf-exec-1");
                assertThat(session.startedAt()).isEqualTo(now.get());
            });
        assertThat(registry.isAttached("s-1")).isTrue();
    }

    @Test
    void testResumeWithinWindowReattachesTheSameSession() {
        registry.register("s-1", webSocketSession("ws-1"), null);
        registry.get("s-1")
            .orElseThrow()
            .engineSessionId(42L);

        registry.detach("s-1");

        assertThat(registry.isAttached("s-1")).isFalse();

        now.set(now.get()
            .plus(Duration.ofSeconds(10)));

        WebSocketSession replacement = webSocketSession("ws-2");

        assertThat(registry.resume("s-1", replacement))
            .get()
            .satisfies(session -> {
                assertThat(WebSocketSessionDecorator.unwrap(session.webSocketSession())).isSameAs(replacement);
                assertThat(session.engineSessionId()).isEqualTo(42L);
                assertThat(session.detachedAt()).isNull();
            });
    }

    @Test
    void testResumeAfterWindowIsRejectedAndForgotten() {
        registry.register("s-1", webSocketSession("ws-1"), null);
        registry.detach("s-1");

        now.set(now.get()
            .plus(VoiceSessionRegistry.RESUME_WINDOW)
            .plusSeconds(1));

        assertThat(registry.resume("s-1", webSocketSession("ws-2"))).isEmpty();
        assertThat(registry.get("s-1")).isEmpty();
    }

    @Test
    void testResumeOfAnAttachedSessionIsRejected() {
        registry.register("s-1", webSocketSession("ws-1"), null);

        assertThat(registry.resume("s-1", webSocketSession("ws-2"))).isEmpty();
    }

    @Test
    void testConcurrentResumeLetsExactlyOneCallerReattach() throws Exception {
        registry.register("s-1", webSocketSession("ws-0"), null);
        registry.detach("s-1");

        int concurrentCallers = 16;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentCallers);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Optional<VoiceSessionRegistry.Session>>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentCallers; i++) {
            final int index = i;

            futures.add(executor.submit(() -> {
                try {
                    if (!startGate.await(5, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Start gate timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                    throw new RuntimeException(e);
                }

                return registry.resume("s-1", webSocketSession("ws-" + index));
            }));
        }

        startGate.countDown();

        int successCount = 0;
        final String[] successfulSocketId = new String[1];

        for (Future<Optional<VoiceSessionRegistry.Session>> future : futures) {
            Optional<VoiceSessionRegistry.Session> result = future.get();

            if (result.isPresent()) {
                successCount++;
                successfulSocketId[0] = result.get()
                    .webSocketSession()
                    .getId();
            }
        }

        executor.shutdown();

        assertThat(successCount).isEqualTo(1);
        assertThat(registry.get("s-1")).get()
            .satisfies(session -> assertThat(session.webSocketSession()
                .getId()).isEqualTo(successfulSocketId[0]));
    }

    private static WebSocketSession webSocketSession(String id) {
        WebSocketSession webSocketSession = mock(WebSocketSession.class);

        when(webSocketSession.getId()).thenReturn(id);

        return webSocketSession;
    }
}
