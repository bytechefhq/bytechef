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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.common.exec.ExecBackend;
import org.springaicommunity.agent.common.exec.ExecHandle;
import org.springaicommunity.agent.common.exec.ExecResult;
import org.springaicommunity.agent.common.exec.ExecSpec;
import org.springaicommunity.agent.exec.docker.DockerCliExecBackend;

/**
 * @author Ivica Cardic
 */
class DockerExecBackendRegistryTest {

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);
    private static final Path FIRST_WORKSPACE = Path.of("tenant_one", "agent");
    private static final Path SECOND_WORKSPACE = Path.of("tenant_two", "agent");

    private final MutableClock clock = new MutableClock();
    private final List<DockerCliExecBackend> createdExecBackends = new ArrayList<>();
    private final Map<DockerCliExecBackend, Path> execBackendWorkspaces = new HashMap<>();
    private final ExecHandle execHandle = mock(ExecHandle.class);

    private final DockerExecBackendRegistry dockerExecBackendRegistry = new DockerExecBackendRegistry(
        this::createExecBackend, IDLE_TIMEOUT, clock);

    @Test
    void testNoContainerStartsBeforeTheFirstCommand() {
        dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE);

        assertThat(createdExecBackends).isEmpty();
    }

    @Test
    void testCommandsOfOneWorkspaceShareItsContainer() {
        ExecBackend execBackend = dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE);

        execBackend.run(ExecSpec.of("pwd"));
        dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE)
            .run(ExecSpec.of("ls"));

        assertThat(createdExecBackends).hasSize(1);
    }

    @Test
    void testEachWorkspaceGetsItsOwnContainer() {
        dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE)
            .run(ExecSpec.of("pwd"));
        dockerExecBackendRegistry.getExecBackend(SECOND_WORKSPACE)
            .run(ExecSpec.of("pwd"));

        assertThat(createdExecBackends).hasSize(2);
        assertThat(execBackendWorkspaces.values()).containsExactlyInAnyOrder(FIRST_WORKSPACE, SECOND_WORKSPACE);
    }

    @Test
    void testAnIdleContainerIsRemoved() {
        dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE)
            .run(ExecSpec.of("pwd"));

        clock.advance(IDLE_TIMEOUT.plusSeconds(1));

        dockerExecBackendRegistry.evictIdle();

        verify(createdExecBackends.getFirst()).close();
    }

    @Test
    void testARecentlyUsedContainerIsKept() {
        dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE)
            .run(ExecSpec.of("pwd"));

        clock.advance(IDLE_TIMEOUT.minusSeconds(1));

        dockerExecBackendRegistry.evictIdle();

        verify(createdExecBackends.getFirst(), never()).close();
    }

    @Test
    void testAContainerWithARunningBackgroundShellIsKept() {
        when(execHandle.isAlive()).thenReturn(true);

        dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE)
            .start(ExecSpec.of("sleep 3600"));

        clock.advance(IDLE_TIMEOUT.plusMinutes(5));

        dockerExecBackendRegistry.evictIdle();

        verify(createdExecBackends.getFirst(), never()).close();
    }

    @Test
    void testAContainerIsKeptWhileACommandIsRunning() {
        AtomicReference<DockerExecBackendRegistry> registryReference = new AtomicReference<>();

        DockerExecBackendRegistry slowCommandRegistry = new DockerExecBackendRegistry(
            workspaceRoot -> {
                DockerCliExecBackend execBackend = createExecBackend(workspaceRoot);

                when(execBackend.run(any())).thenAnswer(invocation -> {
                    clock.advance(IDLE_TIMEOUT.plusMinutes(5));

                    registryReference.get()
                        .evictIdle();

                    return ExecResult.completed(0, "", "");
                });

                return execBackend;
            },
            IDLE_TIMEOUT, clock);

        registryReference.set(slowCommandRegistry);

        slowCommandRegistry.getExecBackend(FIRST_WORKSPACE)
            .run(ExecSpec.of("sleep 900"));

        verify(createdExecBackends.getFirst(), never()).close();
    }

    @Test
    void testARemovedContainerIsStartedAgainForTheNextCommand() {
        ExecBackend execBackend = dockerExecBackendRegistry.getExecBackend(FIRST_WORKSPACE);

        execBackend.run(ExecSpec.of("pwd"));

        clock.advance(IDLE_TIMEOUT.plusSeconds(1));

        dockerExecBackendRegistry.evictIdle();

        execBackend.run(ExecSpec.of("pwd"));

        assertThat(createdExecBackends).hasSize(2);

        verify(createdExecBackends.getLast()).run(any());
    }

    private DockerCliExecBackend createExecBackend(Path workspaceRoot) {
        DockerCliExecBackend execBackend = mock(DockerCliExecBackend.class);

        when(execBackend.run(any())).thenReturn(ExecResult.completed(0, "", ""));
        when(execBackend.start(any())).thenReturn(execHandle);

        createdExecBackends.add(execBackend);
        execBackendWorkspaces.put(execBackend, workspaceRoot);

        return execBackend;
    }

    private static final class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-10-10T10:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
