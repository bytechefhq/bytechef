# WebSocket Handler Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add bidirectional WebSocket handler support to ByteChef component definitions (actions), with WebSocket subflow execution driven by workflow-level trigger configuration.

**Architecture:** Mirror the existing SSE streaming pattern (`SseEmitterHandler`/`StreamPerformFunction`/`SseStreamTaskExecutionPostOutputProcessor`) with a new `WebSocketHandler`/`WebSocketPerformFunction` pair in `ActionDefinition`. The WebSocket subflow (list of tasks to execute during the WS session) is defined at the workflow level as a `websocketTasks` extension on the trigger definition in the workflow JSON — NOT in the component SDK interfaces. `WebhookWebSocketHandler` reads the workflow definition, extracts the subflow definition string from the trigger's extensions, and passes it to `JobSyncExecutor` for execution.

**Tech Stack:** Java 25, Spring Boot 4, Spring WebSocket, component-api SDK, Atlas workflow engine, Caffeine cache, CopyOnWriteArrayList for thread safety.

---

### Task 1: Add WebSocketHandler and WebSocketPerformFunction interfaces to ActionDefinition

**Files:**
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ActionDefinition.java`

**Step 1: Add WebSocketHandler interface after SseEmitterHandler (line ~312)**

Add these interfaces right after the closing brace of `SseEmitterHandler` (after line 312):

```java
/**
 * Functional interface for handling bidirectional WebSocket communication within an action or trigger.
 * Similar to {@link SseEmitterHandler} but supports receiving messages from connected clients
 * in addition to sending data.
 */
@FunctionalInterface
interface WebSocketHandler {

    /**
     * Handles bidirectional WebSocket communication through the provided emitter.
     *
     * @param webSocketEmitter the emitter for sending/receiving WebSocket messages
     */
    void handle(WebSocketEmitter webSocketEmitter);

    /**
     * Represents a bidirectional WebSocket connection that supports sending and receiving
     * both text and binary messages, with lifecycle event listeners.
     */
    interface WebSocketEmitter {

        /**
         * Registers a listener for incoming binary messages.
         *
         * @param binaryMessageListener consumer invoked with binary data received from the client
         */
        void addBinaryMessageListener(Consumer<byte[]> binaryMessageListener);

        /**
         * Registers a listener for connection close events.
         *
         * @param closeListener runnable invoked when the WebSocket connection closes
         */
        void addCloseListener(Runnable closeListener);

        /**
         * Registers a listener for incoming text messages.
         *
         * @param messageListener consumer invoked with deserialized message data from the client
         */
        void addMessageListener(Consumer<Object> messageListener);

        /**
         * Registers a listener for timeout events.
         *
         * @param timeoutListener runnable invoked when a timeout occurs
         */
        void addTimeoutListener(Runnable timeoutListener);

        /**
         * Marks the WebSocket communication as completed.
         */
        void complete();

        /**
         * Marks the WebSocket communication as failed with an error.
         *
         * @param throwable the error that caused the failure
         */
        void error(Throwable throwable);

        /**
         * Sends text/JSON data to the connected WebSocket client.
         *
         * @param data the data to send (will be serialized to JSON)
         */
        void send(Object data);

        /**
         * Sends binary data to the connected WebSocket client.
         *
         * @param data the binary data to send
         */
        void sendBinary(byte[] data);
    }
}
```

**Step 2: Add WebSocketPerformFunction interface after StreamPerformFunction (line ~491)**

Add after the closing brace of `StreamPerformFunction`:

```java
/**
 * Functional interface for executing actions that require bidirectional WebSocket communication.
 * Returns a {@link WebSocketHandler} that bridges the action's WebSocket data to/from connected clients.
 *
 * @see WebSocketHandler
 * @see ActionContext
 * @see Parameters
 */
@FunctionalInterface
interface WebSocketPerformFunction extends BasePerformFunction {

    /**
     * Executes the action and returns a {@link WebSocketHandler} for bidirectional WebSocket communication.
     *
     * @param inputParameters      the input parameters for the action
     * @param connectionParameters the connection parameters for authentication
     * @param context              the action execution context
     * @return the {@link WebSocketHandler} that will handle WebSocket messages
     * @throws Exception if an error occurs during action execution
     */
    WebSocketHandler apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws Exception;
}
```

**Step 3: Add getWebsocketTasks() to ActionDefinition interface (after getWorkflowNodeDescription(), line ~145)**

```java
/**
 * Returns the list of inline Atlas workflow task definitions to execute during a WebSocket session.
 *
 * @return an optional list of task definition maps
 */
Optional<List<Map<String, Object>>> getWebsocketTasks();
```

**Step 4: Add import for Consumer**

Add to the imports section (near line 31):
```java
import java.util.function.Consumer;
```

**Step 5: Run compilation**

Run: `./gradlew :sdks:backend:java:component-api:compileJava`
Expected: PASS (interfaces only, no implementations yet)

**Step 6: Commit**

```bash
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ActionDefinition.java
git commit -m "feat: add WebSocketHandler and WebSocketPerformFunction interfaces to ActionDefinition"
```

---

### Task 2: Add getWebsocketTasks() to TriggerDefinition interface

**Files:**
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/TriggerDefinition.java`

**Step 1: Add getWebsocketTasks() method to TriggerDefinition interface (after getWorkflowSyncExecution(), line ~195)**

```java
/**
 * Returns the list of inline Atlas workflow task definitions to execute during a WebSocket session
 * initiated by this trigger.
 *
 * @return an optional list of task definition maps
 */
Optional<List<Map<String, Object>>> getWebsocketTasks();
```

**Step 2: Run compilation**

Run: `./gradlew :sdks:backend:java:component-api:compileJava`
Expected: PASS (interface method only)

**Step 3: Commit**

```bash
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/TriggerDefinition.java
git commit -m "feat: add getWebsocketTasks() to TriggerDefinition interface"
```

---

### Task 3: Add websocketTasks field and builder methods to ModifiableActionDefinition

**Files:**
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java`

**Step 1: Add private field to ModifiableActionDefinition (after line 297, with other fields)**

```java
private List<Map<String, Object>> websocketTasks;
```

**Step 2: Add perform(WebSocketPerformFunction) builder method (after the StreamPerformFunction overload, line ~443)**

```java
public ModifiableActionDefinition perform(WebSocketPerformFunction perform) {
    this.performFunction = perform;

    return this;
}
```

**Step 3: Add websocketTasks builder method (after the perform methods, before processErrorResponse)**

```java
@SuppressFBWarnings("EI2")
public ModifiableActionDefinition websocketTasks(List<Map<String, Object>> websocketTasks) {
    this.websocketTasks = websocketTasks;

    return this;
}
```

**Step 4: Add getWebsocketTasks() getter (after getWorkflowNodeDescription(), line ~590)**

```java
@Override
public Optional<List<Map<String, Object>>> getWebsocketTasks() {
    return Optional.ofNullable(websocketTasks == null ? null : Collections.unmodifiableList(websocketTasks));
}
```

**Step 5: Add websocketTasks to equals() (line ~498)**

Add `Objects.equals(websocketTasks, that.websocketTasks)` to the existing equals chain.

**Step 6: Add websocketTasks to hashCode() (line ~508)**

Add `websocketTasks` to the `Objects.hash(...)` call.

**Step 7: Add websocketTasks to toString() (line ~600)**

Add `", websocketTasks=" + websocketTasks` to the toString output.

**Step 8: Run compilation**

Run: `./gradlew :sdks:backend:java:component-api:compileJava`
Expected: FAIL — ModifiableTriggerDefinition doesn't implement getWebsocketTasks() yet

**Step 9: Commit (partial — will compile after Task 4)**

```bash
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java
git commit -m "feat: add websocketTasks field and WebSocketPerformFunction support to ModifiableActionDefinition"
```

---

### Task 4: Add websocketTasks field and builder method to ModifiableTriggerDefinition

**Files:**
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java`

**Step 1: Add private field to ModifiableTriggerDefinition (after line 3443, with other fields)**

```java
private List<Map<String, Object>> websocketTasks;
```

**Step 2: Add websocketTasks builder method (after workflowSyncExecution(), line ~3635)**

```java
@SuppressFBWarnings("EI2")
public ModifiableTriggerDefinition websocketTasks(List<Map<String, Object>> websocketTasks) {
    this.websocketTasks = websocketTasks;

    return this;
}
```

**Step 3: Add getWebsocketTasks() getter (after getWorkflowSyncExecution(), line ~3789)**

```java
@Override
public Optional<List<Map<String, Object>>> getWebsocketTasks() {
    return Optional.ofNullable(websocketTasks == null ? null : Collections.unmodifiableList(websocketTasks));
}
```

**Step 4: Add websocketTasks to equals() (line ~3638)**

Add `Objects.equals(websocketTasks, that.websocketTasks)` to the existing equals chain.

**Step 5: Add websocketTasks to hashCode() (line ~3668)**

Add `websocketTasks` to the `Objects.hash(...)` call.

**Step 6: Add websocketTasks to toString() (line ~3792)**

Add `", websocketTasks=" + websocketTasks` to the toString output.

**Step 7: Run full SDK compilation**

Run: `./gradlew :sdks:backend:java:component-api:compileJava`
Expected: PASS — both definitions now implement getWebsocketTasks()

**Step 8: Commit**

```bash
git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java
git commit -m "feat: add websocketTasks field to ModifiableTriggerDefinition"
```

---

### Task 5: Create WebSocketEmitter implementation

**Files:**
- Create: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/WebSocketEmitter.java`
- Reference: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/SseEmitter.java` (mirror this pattern)

**Step 1: Create WebSocketEmitter class**

```java
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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe implementation of {@link WebSocketHandler.WebSocketEmitter} for managing bidirectional
 * WebSocket communication. Mirrors the {@link SseEmitter} pattern with additional support for
 * incoming message listeners and binary data.
 *
 * @author Ivica Cardic
 */
class WebSocketEmitter implements WebSocketHandler.WebSocketEmitter {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEmitter.class);

    private final CopyOnWriteArrayList<Consumer<byte[]>> binaryMessageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> closeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> completionListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Object>> messageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Object>> outboundListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<byte[]>> outboundBinaryListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> timeoutListeners = new CopyOnWriteArrayList<>();
    private final @Nullable Long timeout;

    private volatile boolean completed;
    private volatile boolean errored;
    private volatile @Nullable Throwable lastError;

    WebSocketEmitter() {
        this.timeout = null;
    }

    WebSocketEmitter(Long timeoutMillis) {
        this.timeout = timeoutMillis;
    }

    @Override
    public void addBinaryMessageListener(Consumer<byte[]> binaryMessageListener) {
        binaryMessageListeners.add(binaryMessageListener);
    }

    @Override
    public void addCloseListener(Runnable closeListener) {
        if (completed) {
            try {
                closeListener.run();
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        } else {
            closeListeners.add(closeListener);
        }
    }

    public void addCompletionListener(Runnable callback) {
        if (completed) {
            try {
                callback.run();
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        } else {
            completionListeners.add(callback);
        }
    }

    public void addErrorListener(Consumer<Throwable> consumer) {
        if (errored) {
            try {
                consumer.accept(java.util.Objects.requireNonNull(lastError));
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        } else {
            errorListeners.add(consumer);
        }
    }

    @Override
    public void addMessageListener(Consumer<Object> messageListener) {
        messageListeners.add(messageListener);
    }

    /**
     * Registers a listener for outbound binary data. Called by the platform to intercept
     * binary data sent via {@link #sendBinary(byte[])}.
     */
    public void addOutboundBinaryListener(Consumer<byte[]> listener) {
        outboundBinaryListeners.add(listener);
    }

    /**
     * Registers a listener for outbound data. Called by the platform to intercept
     * data sent via {@link #send(Object)}.
     */
    public void addOutboundListener(Consumer<Object> listener) {
        outboundListeners.add(listener);
    }

    @Override
    public void addTimeoutListener(Runnable listener) {
        if (completed) {
            return;
        }

        timeoutListeners.add(listener);
    }

    @Override
    public void complete() {
        if (completed) {
            return;
        }

        completed = true;

        for (Runnable listener : closeListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }

        for (Runnable listener : completionListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    /**
     * Dispatches an incoming binary message to all registered binary message listeners.
     */
    public void dispatchBinaryMessage(byte[] data) {
        for (Consumer<byte[]> listener : binaryMessageListeners) {
            try {
                listener.accept(data);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    /**
     * Dispatches an incoming text message to all registered message listeners.
     */
    public void dispatchMessage(Object message) {
        for (Consumer<Object> listener : messageListeners) {
            try {
                listener.accept(message);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    @Override
    public void error(Throwable throwable) {
        if (completed) {
            return;
        }

        errored = true;
        lastError = throwable;

        for (var listener : errorListeners) {
            try {
                listener.accept(throwable);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }

        complete();
    }

    public @Nullable Long getTimeout() {
        return timeout;
    }

    @Override
    public void send(Object data) {
        for (var listener : outboundListeners) {
            try {
                listener.accept(data);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    @Override
    public void sendBinary(byte[] data) {
        for (var listener : outboundBinaryListeners) {
            try {
                listener.accept(data);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    public void triggerTimeout() {
        for (Runnable listener : timeoutListeners) {
            try {
                listener.run();
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }
}
```

**Step 2: Run compilation**

Run: `./gradlew :server:libs:platform:platform-job-sync:compileJava`
Expected: PASS

**Step 3: Commit**

```bash
git add server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/WebSocketEmitter.java
git commit -m "feat: add WebSocketEmitter implementation mirroring SseEmitter pattern"
```

---

### Task 6: Create WebSocketStreamTaskExecutionPostOutputProcessor

**Files:**
- Create: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/WebSocketStreamTaskExecutionPostOutputProcessor.java`
- Reference: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/SseStreamTaskExecutionPostOutputProcessor.java` (mirror this pattern)

**Step 1: Create the processor class**

```java
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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes WebSocketHandler output from task execution. Creates a WebSocketEmitter, wires it
 * with listeners that dispatch events to registered SseStreamBridge instances, and blocks
 * until the WebSocket session completes or times out.
 *
 * @author Ivica Cardic
 */
class WebSocketStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger logger =
        LoggerFactory.getLogger(WebSocketStreamTaskExecutionPostOutputProcessor.class);

    private final Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges;

    WebSocketStreamTaskExecutionPostOutputProcessor(
        Cache<String, CopyOnWriteArrayList<SseStreamBridge>> sseStreamBridges) {

        this.sseStreamBridges = sseStreamBridges;
    }

    @Override
    public @Nullable Object process(TaskExecution taskExecution, Object output) {
        if (!(output instanceof ActionDefinition.WebSocketHandler webSocketHandler)) {
            return output;
        }

        long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");

        String key = TenantCacheKeyUtils.getKey(jobId);

        WebSocketEmitter emitter = new WebSocketEmitter();

        emitter.addOutboundListener(payload -> {
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onEvent(payload);
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }
        });

        CountDownLatch latch = new CountDownLatch(1);

        emitter.addCompletionListener(() -> {
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onComplete();
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            latch.countDown();
        });

        emitter.addErrorListener(throwable -> {
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onError(throwable);
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }
        });

        emitter.addTimeoutListener(() -> {
            var bridges = sseStreamBridges.getIfPresent(key);

            if (bridges != null) {
                for (var bridge : bridges) {
                    try {
                        bridge.onError(new TimeoutException("WebSocket stream timed out for job " + jobId));
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            latch.countDown();
        });

        webSocketHandler.handle(emitter);

        try {
            Long timeout = emitter.getTimeout();

            if (timeout == null || timeout < 0) {
                latch.await();
            } else {
                boolean finished = latch.await(timeout, TimeUnit.MILLISECONDS);

                if (!finished) {
                    emitter.triggerTimeout();
                }
            }
        } catch (InterruptedException ignored) {
            Thread thread = Thread.currentThread();

            thread.interrupt();
        }

        return null;
    }
}
```

**Step 2: Register the processor in JobSyncExecutor**

Find `JobSyncExecutor.java` and add the `WebSocketStreamTaskExecutionPostOutputProcessor` alongside the existing `SseStreamTaskExecutionPostOutputProcessor`. Look for where `SseStreamTaskExecutionPostOutputProcessor` is instantiated and add:

```java
new WebSocketStreamTaskExecutionPostOutputProcessor(sseStreamBridges)
```

to the list of post-output processors.

**Step 3: Run compilation**

Run: `./gradlew :server:libs:platform:platform-job-sync:compileJava`
Expected: PASS

**Step 4: Commit**

```bash
git add server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/WebSocketStreamTaskExecutionPostOutputProcessor.java
git add server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/JobSyncExecutor.java
git commit -m "feat: add WebSocketStreamTaskExecutionPostOutputProcessor and register in JobSyncExecutor"
```

---

### Task 7: Create worker-level WebSocketStreamTaskExecutionPostOutputProcessor

**Files:**
- Create: `server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/task/WebSocketStreamTaskExecutionPostOutputProcessor.java`
- Reference: `server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/task/SseStreamTaskExecutionPostOutputProcessor.java` (mirror this pattern)

**Step 1: Create the worker-level processor**

This mirrors the worker-level `SseStreamTaskExecutionPostOutputProcessor` but processes `WebSocketHandler` output instead:

```java
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

package com.bytechef.platform.worker.task;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker-level processor for WebSocketHandler output. Sends events to the message broker
 * for dispatching to the coordinator's SseStreamBridgeRegistry.
 *
 * @author Ivica Cardic
 */
public class WebSocketStreamTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private static final Logger logger =
        LoggerFactory.getLogger(WebSocketStreamTaskExecutionPostOutputProcessor.class);

    private final MessageBroker messageBroker;

    @SuppressFBWarnings("EI")
    public WebSocketStreamTaskExecutionPostOutputProcessor(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public @Nullable Object process(TaskExecution taskExecution, Object output) {
        if (!(output instanceof WebSocketHandler webSocketHandler)) {
            return output;
        }

        long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");
        String tenantId = TenantContext.getCurrentTenantId();

        WebSocketEmitterAdapter emitter = new WebSocketEmitterAdapter();

        emitter.addOutboundListener(
            payload -> sendEvent(jobId, SseStreamEvent.EVENT_TYPE_DATA, payload, tenantId));

        CountDownLatch latch = new CountDownLatch(1);

        emitter.addCompletionListener(() -> {
            sendEvent(jobId, SseStreamEvent.EVENT_TYPE_COMPLETE, null, tenantId);

            latch.countDown();
        });

        emitter.addErrorListener(throwable ->
            sendEvent(jobId, SseStreamEvent.EVENT_TYPE_ERROR, throwable.getMessage(), tenantId));

        emitter.addTimeoutListener(() -> {
            sendEvent(jobId, SseStreamEvent.EVENT_TYPE_ERROR,
                "WebSocket stream timed out for job " + jobId, tenantId);

            latch.countDown();
        });

        webSocketHandler.handle(emitter);

        try {
            Long timeout = emitter.getTimeout();

            if (timeout == null || timeout < 0) {
                latch.await();
            } else {
                boolean finished = latch.await(timeout, TimeUnit.MILLISECONDS);

                if (!finished) {
                    emitter.triggerTimeout();
                }
            }
        } catch (InterruptedException ignored) {
            Thread thread = Thread.currentThread();

            thread.interrupt();
        }

        return null;
    }

    private void sendEvent(long jobId, String eventType, @Nullable Object payload, String tenantId) {
        try {
            SseStreamEvent sseStreamEvent = new SseStreamEvent(jobId, eventType, payload);

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, tenantId);

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }
    }

    private static class WebSocketEmitterAdapter implements WebSocketHandler.WebSocketEmitter {

        private final CopyOnWriteArrayList<Consumer<byte[]>> binaryMessageListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Runnable> closeListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Runnable> completionListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<Object>> messageListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<Object>> outboundListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Consumer<byte[]>> outboundBinaryListeners = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<Runnable> timeoutListeners = new CopyOnWriteArrayList<>();

        private volatile boolean completed;
        private @Nullable Long timeout;

        @Override
        public void addBinaryMessageListener(Consumer<byte[]> binaryMessageListener) {
            binaryMessageListeners.add(binaryMessageListener);
        }

        @Override
        public void addCloseListener(Runnable closeListener) {
            if (completed) {
                closeListener.run();
            } else {
                closeListeners.add(closeListener);
            }
        }

        public void addCompletionListener(Runnable callback) {
            if (completed) {
                callback.run();
            } else {
                completionListeners.add(callback);
            }
        }

        public void addErrorListener(Consumer<Throwable> consumer) {
            errorListeners.add(consumer);
        }

        @Override
        public void addMessageListener(Consumer<Object> messageListener) {
            messageListeners.add(messageListener);
        }

        public void addOutboundBinaryListener(Consumer<byte[]> listener) {
            outboundBinaryListeners.add(listener);
        }

        public void addOutboundListener(Consumer<Object> listener) {
            outboundListeners.add(listener);
        }

        @Override
        public void addTimeoutListener(Runnable listener) {
            timeoutListeners.add(listener);
        }

        @Override
        public void complete() {
            if (completed) {
                return;
            }

            completed = true;

            for (Runnable listener : closeListeners) {
                listener.run();
            }

            for (Runnable listener : completionListeners) {
                listener.run();
            }
        }

        @Override
        public void error(Throwable throwable) {
            if (completed) {
                return;
            }

            for (var listener : errorListeners) {
                listener.accept(throwable);
            }

            complete();
        }

        public @Nullable Long getTimeout() {
            return timeout;
        }

        @Override
        public void send(Object data) {
            for (var listener : outboundListeners) {
                listener.accept(data);
            }
        }

        @Override
        public void sendBinary(byte[] data) {
            for (var listener : outboundBinaryListeners) {
                listener.accept(data);
            }
        }

        public void triggerTimeout() {
            for (Runnable listener : timeoutListeners) {
                listener.run();
            }
        }
    }
}
```

**Step 2: Register the processor**

Find where `SseStreamTaskExecutionPostOutputProcessor` is registered as a bean (likely in a configuration class in platform-worker) and register `WebSocketStreamTaskExecutionPostOutputProcessor` similarly.

**Step 3: Run compilation**

Run: `./gradlew :server:libs:platform:platform-worker:compileJava`
Expected: PASS

**Step 4: Commit**

```bash
git add server/libs/platform/platform-worker/src/main/java/com/bytechef/platform/worker/task/WebSocketStreamTaskExecutionPostOutputProcessor.java
git commit -m "feat: add worker-level WebSocketStreamTaskExecutionPostOutputProcessor"
```

---

### Task 8: Add websocketTasks to TwilioInboundCallTrigger

**Files:**
- Modify: `server/libs/modules/components/twilio/src/main/java/com/bytechef/component/twilio/trigger/TwilioInboundCallTrigger.java`

**Step 1: Add websocketTasks to the trigger definition (after .webhookRequest(), line ~81)**

```java
.websocketTasks(List.of(
    Map.of(
        "type", "twilio/v1/processAudioStream",
        "parameters", Map.of()
    )
))
```

Note: The exact task types will depend on what audio processing actions are available. This is a placeholder that demonstrates the pattern.

**Step 2: Run compilation**

Run: `./gradlew :server:libs:modules:components:twilio:compileJava`
Expected: PASS

**Step 3: Commit**

```bash
git add server/libs/modules/components/twilio/src/main/java/com/bytechef/component/twilio/trigger/TwilioInboundCallTrigger.java
git commit -m "feat: add websocketTasks to TwilioInboundCallTrigger"
```

---

### Task 9: Full compilation and format check

**Step 1: Run spotlessApply**

Run: `./gradlew spotlessApply`
Expected: PASS — all code formatted correctly

**Step 2: Run full compilation**

Run: `./gradlew clean compileJava`
Expected: PASS

**Step 3: Fix any compilation errors**

Address any interface implementation issues across the codebase. The main risk is classes that implement `ActionDefinition` or `TriggerDefinition` but don't yet implement `getWebsocketTasks()`. Search for all implementations:

Run: `grep -rn "implements ActionDefinition" --include="*.java" server/ sdks/` and `grep -rn "implements TriggerDefinition" --include="*.java" server/ sdks/`

Each implementation class needs the `getWebsocketTasks()` method added (returning `Optional.empty()` as default).

**Step 4: Commit**

```bash
git add -A
git commit -m "fix: add getWebsocketTasks() to all ActionDefinition and TriggerDefinition implementations"
```

---

### Task 10: Run tests and regenerate component definition JSONs

**Step 1: Delete existing Twilio definition JSON**

```bash
rm -f server/libs/modules/components/twilio/src/test/resources/definition/twilio_v1.json
rm -rf server/libs/modules/components/twilio/build/resources/test/definition/
```

**Step 2: Run Twilio component tests to regenerate JSON**

Run: `./gradlew :server:libs:modules:components:twilio:test`
Expected: PASS with regenerated JSON

**Step 3: Run full test suite**

Run: `./gradlew test`
Expected: PASS

**Step 4: Commit**

```bash
git add -A
git commit -m "test: regenerate component definition JSONs with websocketTasks field"
```
