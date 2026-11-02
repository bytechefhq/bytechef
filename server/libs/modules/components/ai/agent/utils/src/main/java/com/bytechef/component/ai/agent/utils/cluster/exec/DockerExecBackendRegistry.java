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

package com.bytechef.component.ai.agent.utils.cluster.exec;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.common.exec.ExecBackend;
import org.springaicommunity.agent.common.exec.ExecHandle;
import org.springaicommunity.agent.common.exec.ExecResult;
import org.springaicommunity.agent.common.exec.ExecSpec;
import org.springaicommunity.agent.exec.docker.DockerCliExecBackend;

/**
 * Keeps one {@link DockerCliExecBackend} container per agent workspace, so every Shell Tools call of an agent runs in
 * the same container, and removes a container once it has been unused for the idle timeout and none of its background
 * shells are still running. Containers left behind by a crashed server carry
 * {@link DockerCliExecBackend#CONTAINER_LABEL} and the library removes the rest on JVM shutdown.
 *
 * @author Ivica Cardic
 */
public class DockerExecBackendRegistry {

    private static final Logger log = LoggerFactory.getLogger(DockerExecBackendRegistry.class);

    private static final Duration EVICTION_INTERVAL = Duration.ofMinutes(1);

    private final Clock clock;
    private final Map<Path, Entry> entries = new ConcurrentHashMap<>();
    private final Function<Path, DockerCliExecBackend> execBackendFactory;
    private final Duration idleTimeout;

    public DockerExecBackendRegistry(String dockerCommand, String image, Duration idleTimeout) {
        this(
            workspaceRoot -> DockerCliExecBackend.builder()
                .dockerCommand(dockerCommand)
                .image(image)
                .mount(workspaceRoot, workspaceRoot.toString())
                .environment(Map.of("HOME", workspaceRoot.toString()))
                .shellCommand("/bin/bash", "-c")
                .build(),
            idleTimeout, Clock.systemUTC());

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "agent-utils-docker-eviction");

            thread.setDaemon(true);

            return thread;
        });

        scheduledExecutorService.scheduleWithFixedDelay(
            this::evictIdle, EVICTION_INTERVAL.toSeconds(), EVICTION_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    DockerExecBackendRegistry(
        Function<Path, DockerCliExecBackend> execBackendFactory, Duration idleTimeout, Clock clock) {

        this.clock = clock;
        this.execBackendFactory = execBackendFactory;
        this.idleTimeout = idleTimeout;
    }

    /**
     * Returns the exec backend of the given workspace. Its container starts with the first command and is started again
     * if it was removed as idle in the meantime; the workspace files outlive it.
     */
    public ExecBackend getExecBackend(Path workspaceRoot) {
        return new WorkspaceExecBackend(this, workspaceRoot);
    }

    private Entry acquireEntry(Path workspaceRoot) {
        Instant now = clock.instant();

        return entries.compute(workspaceRoot, (key, entry) -> {
            Entry acquiredEntry = entry == null ? new Entry(execBackendFactory.apply(key), now) : entry;

            acquiredEntry.touch(now);

            acquiredEntry.runningCommands.incrementAndGet();

            return acquiredEntry;
        });
    }

    void evictIdle() {
        Instant expiry = clock.instant()
            .minus(idleTimeout);

        for (Path workspaceRoot : List.copyOf(entries.keySet())) {
            List<Entry> evictedEntries = new ArrayList<>(1);

            entries.computeIfPresent(workspaceRoot, (key, entry) -> {
                if (entry.isIdleSince(expiry)) {
                    evictedEntries.add(entry);

                    return null;
                }

                return entry;
            });

            for (Entry evictedEntry : evictedEntries) {
                evictedEntry.close(workspaceRoot);
            }
        }
    }

    private static final class Entry {

        private final DockerCliExecBackend execBackend;
        private final List<ExecHandle> execHandles = new CopyOnWriteArrayList<>();
        private final AtomicInteger runningCommands = new AtomicInteger();
        private volatile Instant lastUsed;

        private Entry(DockerCliExecBackend execBackend, Instant lastUsed) {
            this.execBackend = execBackend;
            this.lastUsed = lastUsed;
        }

        private void close(Path workspaceRoot) {
            try {
                execBackend.close();
            } catch (RuntimeException runtimeException) {
                log.warn("Failed to remove the shell container of agent workspace {}", workspaceRoot, runtimeException);
            }
        }

        private boolean isIdleSince(Instant expiry) {
            execHandles.removeIf(execHandle -> !execHandle.isAlive());

            return runningCommands.get() == 0 && execHandles.isEmpty() && lastUsed.isBefore(expiry);
        }

        private void touch(Instant instant) {
            lastUsed = instant;
        }
    }

    private record WorkspaceExecBackend(DockerExecBackendRegistry registry, Path workspaceRoot) implements ExecBackend {

        @Override
        public ExecResult run(ExecSpec execSpec) {
            Entry entry = registry.acquireEntry(workspaceRoot);

            try {
                return entry.execBackend.run(execSpec);
            } finally {
                entry.runningCommands.decrementAndGet();

                entry.touch(registry.clock.instant());
            }
        }

        @Override
        public ExecHandle start(ExecSpec execSpec) {
            Entry entry = registry.acquireEntry(workspaceRoot);

            try {
                ExecHandle execHandle = entry.execBackend.start(execSpec);

                entry.execHandles.add(execHandle);

                return execHandle;
            } finally {
                entry.runningCommands.decrementAndGet();
            }
        }
    }
}
