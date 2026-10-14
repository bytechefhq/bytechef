# Remote Task Runners (Kubernetes, AWS Batch) and Kestra-Aligned Runner Model — Design

Date: 2026-10-14
Branch: `0_p_script_runners`

## 1. Goal

Add two Enterprise task runners, **Kubernetes** and **AWS Batch**, to the existing `TaskRunner` SPI (GraalVM, Process,
Docker), and align every runner's trust model with Kestra's so the gating logic gets simpler.

Success criteria:

- A Script or Commands task can select the Kubernetes or AWS Batch runner, pick a connection, and run JavaScript,
  Python, or shell with `input`, `env`, `inputFiles`, `outputFiles` and `timeout`. It returns the same
  `stdout` / `stderr` / `exitCode` / `vars` / output-file result shape as Docker and Process.
- If a worker restarts while a remote job is running, the task re-attaches to that job instead of starting a
  duplicate.
- Docker and Process expose Kestra's property model. Operator gating is reduced to a small, uniform set of switches.

## 2. Decisions

| # | Decision |
|---|---|
| D1 | `run()` stays **blocking**. Internally, remote runners are structured as submit → await → collect, so suspend/resume can be added later without a rewrite. True suspend/resume (releasing the worker permit) is out of scope; Kestra does not have it either. |
| D2 | Kestra-style **`resume`** (re-attach by label after a crash or redelivery) and **`delete`** for Docker, Kubernetes and AWS Batch in v1. |
| D3 | Kubernetes file transfer works like Kestra's: a runner-controlled `fileSidecar` image (default `busybox`) runs as an init container and as an output sidecar. Files move as tar over `pods/exec` into a shared `emptyDir`. The author's image needs nothing special apart from `/bin/sh`. |
| D4 | AWS Batch file transfer goes through an S3 prefix, using presigned URLs. |
| D5 | Credentials come from **ByteChef connections**, using the existing connection infrastructure. Encrypted storage is out of scope (it will be handled for all connections later). |
| D6 | New thin EE components that own only a connection: **`kubernetes`** and **`aws`**. Batch does **not** reuse `awsS3`. The staging bucket is a Batch runner property. |
| D7 | New SPI method `TaskRunner.getConnection()`. The connection is wired under the fixed key `taskRunner` and is **derived on the server when the workflow is saved**. |
| D8 | All runners follow Kestra's model: **the operator decides where code runs; the author decides how it runs there.** Operator switches: per-runner `enabled`, Docker `allow-volume-mounts`, Docker `allow-task-host` (the operator's `host` is the default), GraalVM `trusted-enabled`, and caps on ByteChef's own costs. Everything else is a task property. |
| D9 | Structure: a CE **backend interface** plus a CE **`RemoteTaskRunner` orchestrator**. Docker, Kubernetes and Batch are backends. |
| D10 | Edition split as in Kestra: only the **Kubernetes and AWS Batch runner implementations** and their `kubernetes` / `aws` connection components are EE. GraalVM, Process, Docker, the backend interface, the orchestrator, the connection SPI and the save-time preprocessor are CE. |

## 3. Architecture

### 3.1 Backend interface (CE, `platform-component-runner-api`)

```java
public interface RemoteTaskRunnerBackend {

    String getType();

    String getTitle();

    String getRemoteWorkingDirectory();

    List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties);

    Optional<TaskRunnerConnection> getConnection();

    void validate(TaskRunnerRequest request, RemoteJobContext context);

    Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context);

    RemoteJobHandle submit(
        RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
        throws IOException, InterruptedException;

    RemoteJobExit await(
        RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
        RemoteJobContext context) throws IOException, InterruptedException;

    @Nullable
    InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
        throws IOException, InterruptedException;

    void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context);
}
```

Files move as tar streams. `submit` receives a tar of the local working directory, with entries relative to it.
`collect` returns a tar whose entries are relative to the remote working directory (`output/...` and, optionally,
`.stderr`), or `null` when the job produced no output directory. The orchestrator owns both ends of the local
filesystem: it builds the input tar and extracts the output tar, within `max-transfer-bytes`.

Supporting types, all records or enums in the api module:

- `RemoteJobKey(String value, @Nullable Long jobId)`. `value` is the label/tag value:
  `<tenantId>-<jobId or "none">-<taskExecutionId>` (section 3.4), or a random UUID when no task execution ID exists
  (the editor's Test button).
- `RemoteJobSpec(key, languageId, sourceFileName, interpreter, environment, remoteWorkingDirectory, pullPolicy,
  runnerParameters)`: env is the author env plus `BYTECHEF_*` variables pointing to the **remote** layout. The spec
  carries no deadlines; they are passed to `submit`, `await` and `collect` as method parameters.
- Remote working-directory layout: the same relative layout (`input.json`, `output/`, `.stderr`) under a base
  directory each backend chooses (`/tmp` for Docker, `/bytechef/work` for Kubernetes and Batch), returned by
  `getRemoteWorkingDirectory()`.
- `RemoteJobContext`: the resolved `ComponentConnection` for key `taskRunner` (null for Docker), that runner's
  operator settings as an immutable map, and the task's `host` override (Docker, honoured only when
  `allow-task-host` is on). Backends never read `ApplicationProperties` directly.
- `RemoteJobHandle`: an opaque backend identifier (container ID, pod name plus namespace, Batch job ID plus job
  definition ARN plus S3 prefix).
- `RemoteJobExit(boolean finished, @Nullable Integer exitCode, @Nullable String reason)`.
- `RemoteLogSink`: stdout and stderr appenders, backed by `BoundedOutputCapture`.
- `CleanupMode { DELETE, KILL_AND_DELETE, DETACH }`.
- `TaskRunnerConnection(String componentName, int componentVersion)`.

Contract rules:

- `submit` returns only once the inputs have been delivered and the job is running or queued. When it fails, it
  cleans up whatever it created itself: the orchestrator has no handle to clean up with.
- `find` returns only jobs that are **past the input stage**. A job found before the input stage (for example, a pod
  whose init container is still waiting for `.ready`) is deleted by the backend, and `find` returns empty.
- `collect` returns a tar relative to the remote working directory (`output/...`, optionally `.stderr`), or `null`.
- `cleanup` never throws. Failures are logged at WARN.

These rules are also stated in the interface's Javadoc.

### 3.2 `TaskRunner` SPI change (CE)

```java
default Optional<TaskRunnerConnection> getConnection() {
    return Optional.empty();
}
```

### 3.3 Orchestrator `RemoteTaskRunner` (CE, `platform-component-runner-impl`)

`RemoteTaskRunner implements TaskRunner` wraps one `RemoteTaskRunnerBackend`. `TaskRunnerRegistryImpl` wraps every
`RemoteTaskRunnerBackend` bean in a `RemoteTaskRunner`, so EE modules only provide backend beans and never subclass CE
classes.

The orchestrator contributes the shared properties (section 4.2) after the backend's own, and declares the
capabilities `INLINE_SCRIPT`, `COMMANDS`, `INPUT_FILES`, `OUTPUT_FILES` and `ENVIRONMENT`.

`run(request)`:

1. Common validation: supported language (`TaskRunnerBootstrap.isSupported`), and a connection present when the
   backend declares one. Then `backend.validate`.
2. Resolve deadlines. `waitUntilRunning` defaults to `PT10M`. The completion deadline is the task `timeout` if set,
   otherwise `waitUntilCompletion` (default `PT1H`). Both are clamped to the operator's `max-wait`.
3. Build the key from `TenantContext.getCurrentTenantId()`, `ActionContextAware.getJobId()` and
   `ActionContextAware.getTaskExecutionId()` (section 3.4). If the task execution ID is null, use a random UUID and
   force `resume=false`.
4. Create a `TaskRunnerWorkingDirectory`, then write the source with the bootstrap appended (scripts) or the joined
   commands (commands).
5. `handle = resume ? backend.find(key) : empty`. If empty, call `backend.submit(...)` with a tar of the working
   directory.
6. `exit = backend.await(handle, runningDeadline, completionDeadline, logSink, context)`.
   - Not finished: timeout error with stdout/stderr tails. Nothing is collected on a timeout.
   - Finished with a non-zero or null exit code: a best-effort `collect` within the remaining completion deadline
     (skipped when it has already passed; any collection failure is logged at DEBUG and ignored), so a backend's
     `.stderr` reaches the error. Then a failure error with the exit code, `reason`, and tails. A finished job with a
     null exit code is a failure outcome like a non-zero one, so cleanup honours `delete`.
7. `backend.collect(handle, completionDeadline, context)`, extracting the returned tar into the working directory,
   then `TaskRunnerOutputs.readOutputJson` and `collectOutputFiles`, then build the `TaskRunnerResult`.
8. `finally`, the cleanup mode:

| Outcome | Mode |
|---|---|
| Success or failure (non-zero or null exit code) | `delete ? DELETE : DETACH` |
| Timeout | `KILL_AND_DELETE` |
| `InterruptedException` (graceful shutdown) | `resume ? DETACH : KILL_AND_DELETE`, then restore the interrupt flag |
| Any other exception after submit | `KILL_AND_DELETE` |

A hard crash skips `finally`. The job keeps running, and a redelivered task re-attaches through `find`.

### 3.4 Resume key semantics

The key value is `<tenantId>-<jobId or "none">-<taskExecutionId>`. Task execution IDs are per-tenant
auto-increment values, so the task execution ID alone would collide between tenants on one server or daemon, and a
resumed task could re-attach to another tenant's job and return its output. The tenant ID and the job ID make the key
unique within one ByteChef installation. It is not unique across installations, so two installations should not share
one Docker daemon (or one namespace or queue) while `resume` is on.

Re-attaching needs something to redeliver the task, i.e. a durable message broker. With the default in-memory broker
nothing is redelivered after a restart, so a graceful shutdown with `resume` on leaves the job behind; it is removed
by `delete` on a later run of the same key or by hand (Docker label `io.bytechef.managed-by=bytechef`).

Checked in `TaskExecutionErrorEventListener`:

- A **retry** creates a new `TaskExecution` with a new ID, so it never re-attaches to the failed job.
- A **redelivery** after a crash or shutdown carries the same `TaskExecution` ID, so it re-attaches.

### 3.5 `ActionContextAware.getTaskExecutionId()` (CE)

`ActionDefinitionServiceImpl` already receives `@Nullable Long taskExecutionId`, but `ContextFactoryImpl` maps a
null ID to `0` and `ActionContextImpl` stores a primitive `long`. So today the editor's Test button would yield `0`,
and two concurrent test runs would share resume key `0`.

Add `@Nullable Long getTaskExecutionId()` to `ActionContextAware`. `ActionContextImpl` keeps a boxed
`@Nullable Long` for this getter, set from the raw nullable value; the existing primitive and its `0` default stay
for whatever else reads them. The public SDK `ActionContext` is unchanged; the orchestrator casts to
`ActionContextAware`, as other platform code already does.

### 3.6 Save-time connection derivation (CE)

- New SPI in `atlas-configuration-api`: `WorkflowDefinitionPreprocessor { boolean process(Map<String, Object>
  workflowDefinition); }`. An implementation mutates the map in place and returns whether it changed anything.
  `WorkflowServiceImpl.create` and `update` parse the definition once with `WorkflowDefinitionCodec` (JSON or YAML,
  by `Format`), run the map through all preprocessor beans in `@Order` order, and re-serialise it with
  `WorkflowDefinitionCodec` in the same format only when some preprocessor reported a change, before saving. This is the single path every save goes through: editor parameter edits, Copilot,
  MCP, the REST API, and import.
- `TaskRunnerConnectionWorkflowPreprocessor` lives in `platform-component-runner-impl`, next to the registry. It
  walks the definition map as a tree and reports whether it changed anything. A map is treated as a task by the same rule `WorkflowTaskUtils.getTasks` uses (a
  `name` plus a `type` key), so nested tasks of task dispatchers are found wherever they sit, not only in the
  top-level `tasks[]`. For every task that has a `parameters.taskRunner.type`:
  - **Known, enabled runner with `getConnection()`:** set
    `connections.taskRunner = {componentName, componentVersion, authorizationRequired: true}`.
  - **Known, enabled runner without a connection:** remove `connections.taskRunner`.
  - **Unknown or disabled runner:** leave `connections.taskRunner` unchanged, so switching a runner off and on
    doesn't erase what the user picked.
- If no preprocessor changed the definition, `WorkflowServiceImpl` stores the original string as-is, so unrelated
  saves don't reformat the definition.

## 4. Configuration

### 4.1 Operator settings

Defaults live in `server/apps/server-app/src/main/resources/config/application-bytechef.yml`, never in Java
initializers. The runner type IDs (the `getType()` values, which are also the YAML keys) are `graalvm`, `process`,
`docker`, `kubernetes` and `aws-batch`. Only `graalvm` has an entry today.

```yaml
bytechef:
  script:
    runners:
      graalvm:    { enabled: true,  properties: { trusted-enabled: false } }
      process:    { enabled: false }
      docker:     { enabled: false, properties: { host: "",
                                                  allow-task-host: false, allow-volume-mounts: false } }
      kubernetes: { enabled: false }
      aws-batch:  { enabled: false }
    runner-limits:
      max-wait: PT1H
      max-log-bytes: 1MB
      max-transfer-bytes: 512MB
```

Any runner may override `max-wait`, `max-log-bytes` and `max-transfer-bytes` under its own `properties`.
An empty Docker `host` means docker-java's default daemon (`DOCKER_HOST`, else the local socket). `TaskRunnerLimits`
rejects non-positive values - `max-wait` must be greater than zero, `max-log-bytes` at least 2, `max-transfer-bytes`
greater than zero - with an `IllegalArgumentException` naming the full key
(`bytechef.script.runners.<type>.properties.<key>` for an override, `bytechef.script.runner-limits.<key>` for a global
value).
`runner-limits` is a new nested class on `ApplicationProperties.Script`, with no Java defaults.

**Removed operator settings:** `allowed-images`, `allowed-network-modes`, `allow-host-network`, `default-cpu`,
`max-cpu`, `default-memory`, `max-memory`, `pids-limit`, and the operator gate on Process `inheritEnvironment`.
`DockerOperatorSettings` keeps only `host`, `allow-task-host` and `allow-volume-mounts`.

The registry's startup warnings stay as they are. Two runners (or backends) with the same type fail registry
construction with an `IllegalStateException` naming the type, instead of one silently replacing the other.

### 4.2 Shared remote-runner properties (contributed by the orchestrator)

| Property | Default | Meaning |
|---|---|---|
| `resume` | `true` | Re-attach to a running job that carries this task execution's label |
| `delete` | `true` | Remove the container, pod or job (and S3 prefix) when done |
| `waitUntilRunning` | `PT10M` | Ceiling on scheduling, image pull, and start |
| `waitUntilCompletion` | `PT1H` | Wall-clock ceiling. The task `timeout` takes precedence; both are clamped to `max-wait` |
| `pullPolicy` | `IF_NOT_PRESENT` | `ALWAYS`, `IF_NOT_PRESENT` or `NEVER`. Not used by Batch |

### 4.3 Runner-specific properties

- **Process** (not remote; unchanged apart from the gate): `interpreter`, and `inheritEnvironment` (plain
  property, default `false`).
- **Docker:** `image`, `entrypoint`, `user`, `cpu`, `memory`, `networkMode`, `extraHosts`, `privileged`,
  `credentials` (registry `username` / `password` / `registryToken` / `registry`), `killGracePeriod`.
  - `host`: offered only when `allow-task-host` is on.
  - `volumes`: offered only when `allow-volume-mounts` is on.
  - `validate()` rejects `host` or `volumes` when their switch is off, so workflows created outside the editor are
    rejected too.
- **Kubernetes:** `image`, `namespace` (default from the connection), `serviceAccountName`, `resources` (CPU and
  memory requests and limits), `nodeSelector`, `tolerations`, `labels`, `annotations`, `credentials` (temporary
  imagePullSecret), `fileSidecarImage` (default `busybox`).
- **AWS Batch:** `image`, `jobQueueArn` (required), `bucket` (required), `platform` (`FARGATE` | `EC2`, default
  `FARGATE`), `executionRoleArn` (required for Fargate), `taskRoleArn`, `resources` (vCPU and memory, as the task
  total), `completionCheckInterval` (default `PT5S`), `fileSidecarImage` (default `curlimages/curl`).

## 5. Docker backend (CE)

- Same flow as the current `DockerTaskRunner`, split into the backend steps:
  - `submit`: pull per `pullPolicy`, create the container with labels, tar the inputs in, start it.
  - `await`: follow logs, wait for exit.
  - `collect`: tar `output/` out.
  - `cleanup`: remove the container, or kill it first. On `KILL_AND_DELETE` a non-zero `killGracePeriod` (default
    `PT0S`) stops the container gracefully (SIGTERM, then SIGKILL after the grace period) before it is removed. The
    period is carried on the container as label `io.bytechef.kill-grace-period`, so a resumed handle keeps it.
- `find` looks up containers by the labels `io.bytechef.task-execution-id=<key>` and
  `io.bytechef.managed-by=bytechef`. `cleanup` opens its client with a response timeout of the 30-second client grace
  plus the kill grace period, so a long `killGracePeriod` is not cut short. It resumes only a `running` or `exited`
  container, preferring a running one, and removes never-started `created` containers (a job found before its input
  stage).
- Docker separates stdout and stderr natively, so `.stderr` redirection isn't needed.
- The Kestra-aligned properties replace the clamping and allowlist logic.

## 6. Kubernetes backend (EE)

Module: `server/ee/libs/platform/platform-component/platform-component-runner-kubernetes`. Client: fabric8
`kubernetes-client`, a new entry in `libs.versions.toml`.

### 6.1 Connection `kubernetes/v1`

Component: `server/ee/libs/modules/components/kubernetes`, connection only. Fields:

- `masterUrl` (required)
- `caCertData`
- `token`
- `clientCertData`, `clientKeyData`
- `namespace` (default `default`)
- `insecureSkipTlsVerify` (default `false`)

One `CUSTOM` authorization with all credential fields optional. The backend's `validate` requires either `token` or
both `clientCertData` and `clientKeyData`.

The backend bean is `@ConditionalOnEEVersion`.

### 6.2 Pod layout

```
restartPolicy: Never; activeDeadlineSeconds = completion deadline + 60s
volume  work: emptyDir (sizeLimit = max-transfer-bytes)
init    bytechef-init   (fileSidecarImage)  sh -c 'until [ -f /bytechef/work/.ready ]; do sleep 1; done'
main    main            (image)             sh -c 'exec <interpreter> /bytechef/work/<source> 2>/bytechef/work/.stderr'
sidecar bytechef-output (fileSidecarImage)  sh -c 'until [ -f /bytechef/work/.done ]; do sleep 1; done'
labels  bytechef.io/managed-by=bytechef, bytechef.io/task-execution-id=<key>, bytechef.io/job-id=<jobId>
```

### 6.3 Flow

- **`submit`:**
  1. Create the imagePullSecret if `credentials` is set, then create the pod. Patch the secret with an
     `ownerReference` to the created pod (`v1`/`Pod`, name, uid, `controller: false`, `blockOwnerDeletion: false`)
     so the cluster's garbage collector removes it with the pod; a failed patch is logged and the explicit deletes
     below still apply.
  2. Wait (within `waitUntilRunning`) until `bytechef-init` is running.
  3. Run `sh -c "head -c <size> | tar -xf - -C /bytechef/work"` in `bytechef-init`, with a tar of the local working
     directory (spooled to a temporary file to learn its size) on stdin; `head -c` ends the input because the exec
     protocol cannot signal the end of stdin.
  4. Run `exec touch /bytechef/work/.ready`.
- **`await`:**
  - Follow the `main` container's logs into stdout. Watch the pod until `main` has a terminated state.
  - Take the exit code from `containerStatuses[main].state.terminated`.
  - Fail fast on `ErrImagePull`, `ImagePullBackOff` and `CreateContainerConfigError`.
  - When `waitUntilRunning` expires, report the latest `FailedScheduling` event.
  - Name `OOMKilled` and `Evicted` in `reason`.
- **`collect`:**
  1. Run `exec tar -cf - output .stderr` in `bytechef-output` (working directory `/bytechef/work`), streamed into the
     local working directory and capped at `max-transfer-bytes`. `.stderr` goes to stderr capture.
  2. Run `exec touch /bytechef/work/.done`.
- **`find`:** list pods by `bytechef.io/task-execution-id`. Return one whose `bytechef-init` terminated with exit
  code 0 **and** whose `bytechef-output` is still running (inputs delivered, outputs not yet collected). Delete any
  other match (init still waiting, or output sidecar already terminated) and return empty.
- **`cleanup`:**
  - `DELETE`: delete the pod at once (grace period 0, because the job has already finished), and the secret if one
    was created.
  - `KILL_AND_DELETE`: the same, with `killGracePeriod` (default 0) as the pod's grace period.
  - `DETACH`: no-op.
  - The deletes run with the thread's interrupt flag cleared and restore it afterwards, so an interrupted worker still
    removes its pod and secret. The pull secret's owner reference removes it with the pod even if its own delete
    fails.
- **Interrupts:** fabric8 turns an interrupt inside an HTTP call into a `KubernetesClientException` caused by an
  `InterruptedIOException` and re-sets the flag. The backend reports it as an `InterruptedException` (from `find`,
  which cannot throw one, as an `IllegalStateException` with the flag restored), so the orchestrator detaches the job
  when `resume` is on instead of recording an error and killing it.

### 6.4 Required RBAC (documented)

- `pods`: create, get, list, watch, delete
- `pods/log`: get
- `pods/exec`: create, get
- `secrets`: create, patch, delete (only when `credentials` is used)

## 7. AWS Batch backend (EE)

Module: `server/ee/libs/platform/platform-component/platform-component-runner-aws-batch`. Uses the AWS SDK v2
`batch`, `s3`, `cloudwatchlogs` and `sts` clients through the existing BOM. v1 supports ECS-backed queues only, on
Fargate or EC2.

### 7.1 Connection `aws/v1`

Component: `server/ee/libs/modules/components/aws`, connection only (no existing component uses the name `aws`).
Fields: `accessKeyId`, `secretAccessKey`, `region` (required), `roleArn`, `externalId`.

The backend bean is `@ConditionalOnEEVersion`. The AWS SDK version comes from the Spring Cloud AWS 4.2.0 BOM already
applied in `server/build.gradle.kts`, which is past the 2.25 release that introduced multi-container Batch jobs
(`ecsProperties`).

- With `roleArn`, the backend wraps the static keys in an `StsAssumeRoleCredentialsProvider`, with a session
  duration that covers the deadline (at most 12 h).
- There is no endpoint override.

### 7.2 Staging

- Prefix: `s3://<bucket>/bytechef/<jobId>/<key>/`.
- Uploads `input.tar`.
- Presigns a GET for `input.tar` and a PUT for `output.tar`. Both are valid for the completion deadline plus 10 min,
  and are passed as the env variables `BYTECHEF_INPUT_URL` and `BYTECHEF_OUTPUT_URL`.
- `validate` rejects a deadline the credentials can't cover (assumed-role sessions are limited to 12 h).

### 7.3 Job definition (registered per run, because Batch cannot override the image at submit time)

```
type: container, platformCapabilities: [FARGATE|EC2], ecsProperties.taskProperties:
  volumes: work (ephemeral)
  bytechef-init   (fileSidecarImage, essential=false)
      sh -c 'curl -fsS "$BYTECHEF_INPUT_URL" | tar -xf - -C /bytechef/work'
  main            (image, essential=false)          dependsOn bytechef-init=SUCCESS
      sh -c 'exec <interpreter> /bytechef/work/<source> 2>/bytechef/work/.stderr'
  bytechef-output (fileSidecarImage, essential=true) dependsOn main=COMPLETE
      sh -c 'cd /bytechef/work && tar -cf output.tar output .stderr && curl -fsS -T output.tar "$BYTECHEF_OUTPUT_URL"'
  logConfiguration: awslogs; executionRoleArn / taskRoleArn from properties
jobName = bytechef-<key>; tags = bytechef.io/managed-by, task-execution-id, job-id
```

Why the `essential` flags are set this way: when an ECS essential container stops, ECS stops every other container
in the task. If `main` were essential, the task would be torn down the moment the script exited, before
`bytechef-output` had uploaded anything. So `bytechef-output` is the only essential container, and the task ends
when it finishes. Success is therefore **not** read from the Batch job status; it comes from `main`'s exit code in
`DescribeJobs` (`ecsProperties.taskProperties[].containers[].exitCode`). If `bytechef-init` fails, `main` never
starts, the essential container never starts, and the task fails with a `statusReason`, which `await` reports.

`output.tar` is written to the volume before the upload because S3 rejects a presigned PUT without a
`Content-Length` (a streamed `curl -T -` sends chunked encoding, which S3 answers with 501).

Fargate resources: `resources` is the task total. Each sidecar reserves 0.25 vCPU / 64 MiB, which is subtracted from
`main`, so the total stays a valid Fargate CPU/memory combination. `validate` rejects totals that leave `main` with
less than 0.25 vCPU / 128 MiB, or that aren't a valid Fargate combination.

### 7.4 Flow

- **`submit`:** tar the inputs, `PutObject`, presign, `RegisterJobDefinition`, then `SubmitJob` to `jobQueueArn`.
- **`await`:**
  - Poll `DescribeJobs` every `completionCheckInterval`.
    - `SUBMITTED`, `PENDING`, `RUNNABLE` and `STARTING` count against `waitUntilRunning`. Running out there means
      the queue has no capacity, and the error says so.
    - `RUNNING` counts against the completion deadline.
  - Poll CloudWatch `GetLogEvents` for `main`'s log stream (using `nextForwardToken`) into stdout.
  - Terminal states: `SUCCEEDED` or `FAILED`. In both cases the result exit code is `main`'s container exit code; a
    `FAILED` job whose `main` never ran reports the job's `statusReason`.
- **`collect`:** `GetObject output.tar`, capped at `max-transfer-bytes`, extracted into the local working directory.
  `.stderr` goes to stderr capture.
- **`find`:** `ListJobs` on the queue with filter `JOB_NAME = bytechef-<key>`, returning the newest non-terminal
  job. Inputs are uploaded before submit, so every job found is past the input stage.
- **`cleanup`:**
  - `DELETE`: `DeregisterJobDefinition`, then delete the S3 prefix.
  - `KILL_AND_DELETE`: `TerminateJob` first, then the same.
  - `DETACH`: no-op.

### 7.5 Required IAM for the connection's principal (documented)

- `batch:RegisterJobDefinition`, `DeregisterJobDefinition`, `SubmitJob`, `DescribeJobs`, `ListJobs`,
  `TerminateJob`, `TagResource`
- `s3:PutObject`, `GetObject`, `DeleteObject`, `ListBucket` on the bucket prefix
- `logs:GetLogEvents`
- `iam:PassRole` for the execution and task roles

## 8. Error handling

- **Validation:** `IllegalArgumentException`, thrown before anything is submitted. The missing-connection message is
  "The <Title> runner needs a connection; select one in the task's connections".
- **Runtime failures:** `IllegalStateException` naming the runner, the phase (`submitting`, `waiting to run`,
  `running`, `collecting`), the exit code or `reason`, and the stdout/stderr tails.
- **Remote API errors:** fabric8 `KubernetesClientException` and AWS `SdkException` are wrapped with their HTTP
  status or AWS error code. Connection parameters, tokens, presigned URLs and secrets never appear in messages or
  logs.
- **Cleanup failures:** logged at WARN. They never replace the original error.
- **Transfer caps:** exceeding `max-transfer-bytes` while uploading or downloading stops that transfer and fails the
  task with a message naming the cap.

## 9. Testing

- **api:** record validation for the new types.
- **impl:**
  - `RemoteTaskRunnerTest` against a fake backend: deadline clamping, the cleanup-mode table, the resume branches,
    the null task execution ID path, and error messages.
  - `TaskRunnerConnectionWorkflowPreprocessorTest`: set, remove and leave unchanged; nested tasks; unchanged
    definitions returned identically.
  - Docker: the existing `DockerTaskRunnerIntTest` is extended with resume (re-attach to a labelled container) and
    `delete=false`.
- **atlas:** `WorkflowServiceIntTest` covers preprocessors being applied on create and update.
- **Kubernetes (EE):**
  - Unit test on fabric8 `KubernetesMockServer`: pod spec shape, `find` branches, error mapping.
  - `KubernetesRemoteTaskRunnerBackendIntTest` on Testcontainers **k3s**: a Python script, a shell `commands` task,
    input and output files, a timeout, and resume.
- **AWS Batch (EE):**
  - Unit tests with mocked `BatchClient`, `S3Client` and `CloudWatchLogsClient`: job definition shape, Fargate
    arithmetic, status mapping, resume, cleanup modes, and the presign/credential-lifetime check.
  - LocalStack integration test for the S3 staging round-trip only.
  - End-to-end Batch is a documented manual check.
- **Definition snapshots:** regenerated for the Script and Commands components (taskRunner property changes) and for
  the new `kubernetes` and `aws` components.

## 10. Documentation

- `script` and `commands` READMEs: runner tables cover the shared properties, Kestra-aligned Docker/Process, the two
  runners marked Enterprise, and the `/bin/sh` requirement for remote images.
- `kubernetes` and `aws` component READMEs: connection fields, RBAC/IAM, and an example.
- The generated reference pages pick up the new runners through the existing path that describes every runner.

## 11. Delivery plan (branch `0_p_script_runners`, unpushed; folding allowed)

| Task | Commit |
|---|---|
| 1 | fixup → `Add the task runner SPI` (`e9c39fa8e4f`) |
| 2 | new: `Add the task runner limits` |
| 3 | new: `Add the remote task runner backend SPI` |
| 4 | fixup → `Add bounded output capture for external executions` (`819baff548b`) |
| 5 | new: `Expose the task execution id on the action context` |
| 6 | new: `Add the remote task runner orchestrator`; new: `Collect a failed remote job's stderr before reporting it` |
| 7 | new: `Offer the process runner's inherit environment option to every workflow` |
| 8 | new: `Run the Docker runner through the remote task runner orchestrator`; new: `Give the docker runner's kill grace period and resume their intended effect` |
| 9 | new: `Add the workflow definition preprocessor` |
| 10 | new: `Derive the task runner connection when a workflow is saved` |
| 11 | rebase: place the fixups, verify every commit builds. Besides Tasks 1 and 4, two formatting-only fixups land here: `GraalVmTaskRunnerTest` → `Add env, input files, output files and timeout to the script actions` (`e89851b4e78`), and `TaskRunnerPropertyFactoryTest` → `Choose the default task runner by a declared preference, not by sort order` (`cf7e9d9ede2`) |

Docker is reworked in a new commit rather than folded into `411d6633529`, because the orchestrator needs
`ContainerArchive`, which that commit introduces. The Kubernetes and AWS Batch backends (sections 6 and 7) are not
part of this core delivery and follow as new commits.

Known gap, not addressed here: in the EE distributed deployment, `configuration-app` saves workflows without the
runner registry on its classpath, so the connection preprocessor would not run there.

## 12. Out of scope / follow-ups

- True suspend/resume that releases the `workerExecutor` permit during remote jobs.
- Kestra-style `podSpec` / `containerSpec` merge overrides.
- EKS-backed Batch queues; auto-creating Batch job queues.
- Native Kubernetes sidecars (`restartPolicy: Always` init containers, 1.29+).
- Optional operator allowlist of Kubernetes API hosts (`allowed-api-hosts`) as SSRF hardening.
- CloudWatch Live Tail streaming.
- Other EE runners: Azure Batch, Google Batch, Cloud Run, EC2, GCE, Azure VM.
