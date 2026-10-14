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

package com.bytechef.platform.component.runner.remote;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Runs one task somewhere else. The orchestrator owns the local working directory, the deadlines and the log capture; a
 * backend only moves a tar in, starts the job, reports how it ended, and moves a tar out.
 */
public interface RemoteTaskRunnerBackend {

    /**
     * Returns the runner type identifier this backend is registered under.
     */
    String getType();

    /**
     * Returns the human readable name of this backend.
     */
    String getTitle();

    /**
     * Returns the absolute directory on the remote side under which every job's working directory is created.
     */
    String getRemoteWorkingDirectory();

    /**
     * Returns the backend's configuration properties, given the operator's properties for this runner type.
     */
    List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties);

    /**
     * Returns the connection component this backend needs, if any.
     */
    Optional<TaskRunnerConnection> getConnection();

    /**
     * Validates that the request can run on this backend, throwing when it cannot.
     */
    void validate(TaskRunnerRequest request, RemoteJobContext context);

    /**
     * Looks up a previously submitted job by its key, so a redelivered task can reattach to it. Returns only a job that
     * is past the input stage (its inputs fully delivered); a job found before that stage is removed and not returned,
     * so the caller submits afresh.
     */
    Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context);

    /**
     * Delivers the input archive and starts the job. Returns once the inputs are delivered and the job is running or
     * queued; {@code runningDeadline} bounds scheduling, image pull and start. When submit fails it must clean up
     * whatever it created itself, because the orchestrator has no handle to clean up with.
     */
    RemoteJobHandle submit(
        RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
        throws IOException, InterruptedException;

    /**
     * Waits for the job to end, streaming its logs into {@code logSink}. {@code runningDeadline} bounds scheduling,
     * image pull and start; {@code completionDeadline} bounds the whole job. Returns an unfinished exit when a deadline
     * passes first.
     */
    RemoteJobExit await(
        RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
        RemoteJobContext context) throws IOException, InterruptedException;

    /**
     * Returns a tar whose entries are relative to the remote working directory ({@code output/...} and optionally
     * {@code .stderr}), or {@code null} when the job produced no output directory.
     */
    @Nullable
    InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
        throws IOException, InterruptedException;

    /**
     * Releases the job's remote resources according to {@code cleanupMode}. Must not throw; a failure is logged and
     * swallowed.
     */
    void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context);
}
