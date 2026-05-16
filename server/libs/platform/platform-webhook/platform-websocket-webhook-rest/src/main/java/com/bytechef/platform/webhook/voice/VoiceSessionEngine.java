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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs a voice session's {@code websocketTasks} pipeline.
 *
 * <p>
 * A voice session is not a workflow run and this engine is why it no longer pretends to be one. The trigger hands off
 * here without starting a job; the pipeline's stages run concurrently for as long as the caller is connected; and Atlas
 * re-enters only when the session ends, through the continuation job the WS handler creates. Nothing about a live call
 * flows through the task engine.
 *
 * <p>
 * That removes an entire class of impedance mismatch. Atlas dispatches a workflow's tasks one after another and treats
 * "task complete" as "advance the workflow", but a voice stage does not complete until the caller hangs up — so a
 * multi-stage pipeline could never get past its first stage, and the stage that did run pinned a worker thread for the
 * length of the call. Here every stage starts at once and none of them occupies a pool thread: the components are
 * callback-driven, and the session is kept alive by the WebSocket, not by a blocked task.
 *
 * <p>
 * Stage order in the definition IS the wiring, exactly as before — inbound audio goes to the first stage, each stage's
 * outbound feeds the next, and the last stage's outbound reaches the caller. Emitters are registered and the chain is
 * wired BEFORE any component runs, so a component that speaks the moment it starts (a greeting, say) cannot emit into
 * an unwired pipeline.
 *
 * <p>
 * <b>Known gap:</b> LLM usage during a call is not cost-attributed. {@code ActionDefinitionFacade} publishes
 * {@code WorkflowLlmUsageEvent} only when it has a job id, and a session has none. Post-call work, running in the
 * continuation job, is tracked normally.
 *
 * @author Ivica Cardic
 */
@Component
public class VoiceSessionEngine {

    private static final Logger log = LoggerFactory.getLogger(VoiceSessionEngine.class);

    private static final String CONNECTIONS = "connections";
    private static final String CONNECTION_ID = "connectionId";
    private static final String NAME = "name";
    private static final String PARAMETERS = "parameters";
    private static final String TASKS = "tasks";
    private static final String TYPE = "type";

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final Evaluator evaluator;
    private final WebSocketEmitterRegistry webSocketEmitterRegistry;
    private final WebSocketTaskChain webSocketTaskChain;

    private final AtomicLong sessionIds = new AtomicLong();
    private final Cache<Long, List<String>> stageNamesBySessionId;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public VoiceSessionEngine(
        ActionDefinitionFacade actionDefinitionFacade, Evaluator evaluator,
        WebSocketEmitterRegistry webSocketEmitterRegistry, WebSocketTaskChain webSocketTaskChain) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.evaluator = evaluator;
        this.webSocketEmitterRegistry = webSocketEmitterRegistry;
        this.webSocketTaskChain = webSocketTaskChain;
        this.stageNamesBySessionId = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
    }

    /**
     * Starts every stage of the pipeline and wires them into the caller's session.
     *
     * @param pipelineDefinition the {@code websocketTasks} definition carried on the trigger
     * @param inputs             session values the stages' parameters may reference, above all {@code callSid}
     * @param environmentId      the environment the components resolve connections in
     * @param type               the platform type (AUTOMATION or EMBEDDED)
     * @param terminalBridge     receives the LAST stage's emitter, so the caller can attach the WS session
     * @return the started session
     */
    public VoiceSession start(
        String pipelineDefinition, Map<String, ?> inputs, @Nullable Long environmentId, @Nullable PlatformType type,
        Consumer<WebSocketEmitter> terminalBridge) {

        List<Stage> stages = parseStages(pipelineDefinition);

        if (stages.isEmpty()) {
            throw new IllegalArgumentException("Voice pipeline declares no stages");
        }

        long sessionId = sessionIds.incrementAndGet();

        List<String> stageNames = stages.stream()
            .map(Stage::name)
            .toList();

        // Register every emitter and wire the chain before any component runs: a component may greet the caller
        // the moment it starts, and an unwired pipeline would drop that audio on the floor.
        for (Stage stage : stages) {
            webSocketEmitterRegistry.register(sessionId, stage.name(), new WebSocketEmitter());
        }

        webSocketTaskChain.wire(sessionId, stageNames, terminalBridge);

        stageNamesBySessionId.put(sessionId, stageNames);

        try {
            for (Stage stage : stages) {
                startStage(sessionId, stage, inputs, environmentId, type);
            }
        } catch (RuntimeException runtimeException) {
            stop(sessionId);

            throw runtimeException;
        }

        log.info("Voice session started: sessionId={}, stages={}", sessionId, stageNames);

        return new VoiceSession(sessionId, stageNames);
    }

    /**
     * Ends a session: every stage's emitter is completed, which is what tells the components to close their provider
     * connections, and the session's emitters are dropped. Safe to call for an unknown or already-stopped session.
     */
    public void stop(long sessionId) {
        List<String> stageNames = stageNamesBySessionId.getIfPresent(sessionId);

        if (stageNames == null) {
            return;
        }

        for (String stageName : stageNames) {
            webSocketEmitterRegistry.get(sessionId, stageName)
                .ifPresent(emitter -> {
                    try {
                        emitter.complete();
                    } catch (Exception exception) {
                        log.warn("Failed to complete stage emitter: sessionId={}, stage={}", sessionId, stageName,
                            exception);
                    }
                });
        }

        webSocketEmitterRegistry.unregisterAll(sessionId);
        stageNamesBySessionId.invalidate(sessionId);

        log.info("Voice session stopped: sessionId={}", sessionId);
    }

    private void startStage(
        long sessionId, Stage stage, Map<String, ?> inputs, @Nullable Long environmentId,
        @Nullable PlatformType type) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(stage.type());

        // No job means nothing else evaluates the stage's parameters, so ${callSid} and friends are resolved here
        // against the session's inputs.
        Map<String, ?> inputParameters = evaluator.evaluate(stage.parameters(), inputs);

        Object output = actionDefinitionFacade.executePerform(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), null, null, null, null,
            null, inputParameters, stage.connectionIds(), stage.extensions(), environmentId, type, false, null, null,
            null);

        if (!(output instanceof WebSocketHandler webSocketHandler)) {
            throw new IllegalStateException(
                "Voice pipeline stage '" + stage.name() + "' (" + stage.type() +
                    ") is not a realtime action - its perform returned " +
                    (output == null ? "null" : output.getClass()
                        .getName()));
        }

        webSocketEmitterRegistry.get(sessionId, stage.name())
            .ifPresent(webSocketHandler::handle);
    }

    @SuppressWarnings("unchecked")
    private static List<Stage> parseStages(String pipelineDefinition) {
        Map<String, ?> definition = (Map<String, ?>) JsonUtils.read(pipelineDefinition, Map.class);

        List<Stage> stages = new ArrayList<>();

        for (Map<?, ?> taskMap : MapUtils.getList(definition, TASKS, Map.class, List.of())) {
            stages.add(toStage((Map<String, ?>) taskMap));
        }

        return stages;
    }

    @SuppressWarnings("unchecked")
    private static Stage toStage(Map<String, ?> taskMap) {
        String name = MapUtils.getRequiredString(taskMap, NAME);
        String type = MapUtils.getRequiredString(taskMap, TYPE);

        Map<String, ?> parameters = MapUtils.getMap(taskMap, PARAMETERS, Map.of());

        Map<String, Long> connectionIds = new LinkedHashMap<>();

        Map<String, ?> connections = MapUtils.getMap(taskMap, CONNECTIONS, Map.of());

        for (Map.Entry<String, ?> entry : connections.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> connectionMap) {
                Object connectionId = ((Map<String, ?>) connectionMap).get(CONNECTION_ID);

                if (connectionId != null) {
                    connectionIds.put(entry.getKey(), Long.valueOf(String.valueOf(connectionId)));
                }
            }
        }

        // Everything that is not a recognised task member is node state the component reads beside its input —
        // above all clusterElements, which is how an AI agent stage finds its model.
        Map<String, Object> extensions = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : taskMap.entrySet()) {
            String key = entry.getKey();

            if (!NAME.equals(key) && !TYPE.equals(key) && !PARAMETERS.equals(key) && !CONNECTIONS.equals(key)) {
                extensions.put(key, entry.getValue());
            }
        }

        return new Stage(name, type, parameters, connectionIds, extensions);
    }

    /**
     * A started voice session: its id, and its stage names in declaration order — that order IS the pipeline wiring.
     */
    public record VoiceSession(long sessionId, List<String> stageNames) {

        public VoiceSession {
            stageNames = List.copyOf(stageNames);
        }
    }

    private record Stage(
        String name, String type, Map<String, ?> parameters, Map<String, Long> connectionIds,
        Map<String, ?> extensions) {
    }
}
