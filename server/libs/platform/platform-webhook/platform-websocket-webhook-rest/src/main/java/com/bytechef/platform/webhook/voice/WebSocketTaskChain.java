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

package com.bytechef.platform.webhook.voice;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Wires a WebSocket sub-workflow's tasks into a linear pipeline: each task's outbound feeds the next task's inbound
 * dispatch, and the last task's outbound is handed to a caller-supplied terminal bridge (the WS session).
 *
 * <p>
 * Task order in the sub-workflow IS the wiring — there is no {@code subscribesTo}/{@code publishesTo} DSL. The first
 * task's inbound comes from the WS handler forwarding session frames to its registered emitter by name.
 *
 * <p>
 * The chain is built optimistically: an {@code awaitEmitter} callback is registered per task and fires when that task's
 * emitter is registered. Callers may wire before or after registering — the {@code CompletableFuture}-backed registry
 * handles both orders. {@code VoiceSessionEngine} registers every emitter first and wires before any component runs, so
 * a component that greets the caller on start cannot emit into an unwired pipeline.
 *
 * <p>
 * Lives here, next to the registry it reads, rather than in either WS handler: both handlers need the identical wiring,
 * and a copy in each is a copy that can drift. It also makes the wiring reachable from tests that drive a real
 * {@link JobSyncExecutor} instead of re-implementing the chain.
 *
 * @author Ivica Cardic
 */
@Component
public class WebSocketTaskChain {

    private final WebSocketEmitterRegistry webSocketEmitterRegistry;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WebSocketTaskChain(WebSocketEmitterRegistry webSocketEmitterRegistry) {
        this.webSocketEmitterRegistry = webSocketEmitterRegistry;
    }

    /**
     * Chains the given tasks for one sub-flow.
     *
     * @param jobId          the sub-flow's job id
     * @param taskNames      the sub-workflow's task names, in declaration order
     * @param terminalBridge invoked with the LAST task's emitter, so the caller can attach its own session-facing
     *                       listeners (Twilio media framing, plain binary frames, and so on)
     */
    public void wire(long jobId, List<String> taskNames, Consumer<WebSocketEmitter> terminalBridge) {
        if (taskNames.isEmpty()) {
            return;
        }

        for (int index = 0; index < taskNames.size(); index++) {
            String name = taskNames.get(index);
            boolean last = index == taskNames.size() - 1;
            String nextName = last ? null : taskNames.get(index + 1);

            webSocketEmitterRegistry.awaitEmitter(jobId, name)
                .thenAccept(emitter -> {
                    if (last) {
                        terminalBridge.accept(emitter);
                    } else {
                        webSocketEmitterRegistry.awaitEmitter(jobId, nextName)
                            .thenAccept(nextEmitter -> {
                                emitter.addOutboundListener(nextEmitter::dispatchMessage);
                                emitter.addOutboundBinaryListener(nextEmitter::dispatchBinaryMessage);
                                // Forward cancelTurn DOWNSTREAM: when this task cancels a turn, the next
                                // task hears about it and aborts whatever it was doing for that turn.
                                emitter.addOutboundTurnCancelListener(nextEmitter::cancelTurn);
                            });
                    }
                });
        }

        // Wire UPSTREAM cancelTurn propagation. When any task in the chain cancels a turn, propagate the
        // cancel to every other task so STT/agent/TTS all abort coherently. The simplest and correct policy
        // is "broadcast across the chain" — chain length is small (1-4) so the fan-out cost is negligible.
        for (int index = 0; index < taskNames.size(); index++) {
            String sourceName = taskNames.get(index);

            for (int otherIndex = 0; otherIndex < taskNames.size(); otherIndex++) {
                if (otherIndex == index) {
                    continue;
                }

                String targetName = taskNames.get(otherIndex);

                webSocketEmitterRegistry.awaitEmitter(jobId, sourceName)
                    .thenAccept(sourceEmitter -> webSocketEmitterRegistry.awaitEmitter(jobId, targetName)
                        .thenAccept(targetEmitter -> sourceEmitter.addOutboundTurnCancelListener(
                            targetEmitter::cancelTurn)));
            }
        }
    }
}
