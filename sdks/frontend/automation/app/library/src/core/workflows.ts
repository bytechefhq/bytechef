import type {CallOptions} from './connections';
import {BytechefAppError} from './errors';
import type {Requester} from './request';
import {expectArray, expectObject} from './response';
import type {AppWorkflow, WorkflowRun, WorkflowRunStatus} from './types';

export type WaitMode = false | true | 'approval';

export interface ExecuteWorkflowOptions extends CallOptions {
    /**
     * Delays between polls in milliseconds; the last one repeats. An empty list means the defaults; each delay is at
     * least 100 ms.
     */
    pollDelaysMs?: number[];
    /**
     * `true` polls until the run finishes or needs approval; `'approval'` also waits through approvals. Neither has a
     * time limit, so pass `signal` to bound the wait; each request is still bounded by the client timeout.
     */
    wait?: WaitMode;
    /**
     * How long the server holds the first request before answering with a running run. Must be shorter than the
     * client timeout (`timeoutMs`): otherwise `execute` throws a `RangeError` and sends nothing, because the starting
     * request would time out with no `runId` while the run had already started. When omitted and the server's default
     * hold (`SERVER_DEFAULT_WAIT_SECONDS`) would not leave `WAIT_HEADROOM_SECONDS` before `timeoutMs`, `execute` sends
     * `timeoutMs` in whole seconds minus that headroom instead (never below 0).
     */
    waitSeconds?: number;
}

export const DEFAULT_POLL_DELAYS_MS = [1_000, 2_000, 3_000, 4_000, 5_000];

export const MIN_POLL_DELAY_MS = 100;

/** The server's hold when `waitSeconds` is omitted (`bytechef.app.workflow.default-wait`). */
export const SERVER_DEFAULT_WAIT_SECONDS = 30;

/** Time left between the end of the server's hold and the client timeout, so the answer still arrives in time. */
export const WAIT_HEADROOM_SECONDS = 5;

export interface WorkflowsApi {
    /**
     * Starts a run and, depending on `wait`, polls it. An aborted wait throws `ABORTED` with `runId`, and a failed
     * poll keeps its code, message and status and gains `runId`, so the caller can resume with `getRun`. A run that
     * failed is returned as data, not thrown.
     */
    execute(
        workflowKey: string,
        input: Record<string, unknown>,
        options?: ExecuteWorkflowOptions
    ): Promise<WorkflowRun>;
    getRun(runId: string, options?: CallOptions): Promise<WorkflowRun>;
    list(options?: CallOptions): Promise<AppWorkflow[]>;
}

export function isFinished(status: WorkflowRunStatus): boolean {
    return status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED';
}

export function defaultSleep(ms: number, signal?: AbortSignal): Promise<void> {
    return new Promise((resolve, reject) => {
        if (signal?.aborted) {
            reject(signal.reason);

            return;
        }

        const timer = setTimeout(() => {
            signal?.removeEventListener('abort', onAbort);
            resolve();
        }, ms);

        function onAbort() {
            clearTimeout(timer);
            reject(signal!.reason);
        }

        signal?.addEventListener('abort', onAbort, {once: true});
    });
}

function shouldKeepWaiting(run: WorkflowRun, wait: WaitMode): boolean {
    if (wait === false || isFinished(run.status)) {
        return false;
    }

    return run.status !== 'APPROVAL_REQUIRED' || wait === 'approval';
}

export function createWorkflowsApi(
    request: Requester,
    getTimeoutMs: () => number,
    sleep: (ms: number, signal?: AbortSignal) => Promise<void> = defaultSleep
): WorkflowsApi {
    async function getRun(runId: string, options: CallOptions = {}): Promise<WorkflowRun> {
        const path = `/workflow-runs/${encodeURIComponent(runId)}`;

        return expectObject<WorkflowRun>(await request({method: 'GET', path, signal: options.signal}), path);
    }

    return {
        async execute(workflowKey, input, options = {}) {
            const body: Record<string, unknown> = {input};

            if (options.waitSeconds !== undefined) {
                if (options.waitSeconds * 1000 >= getTimeoutMs()) {
                    throw new RangeError('waitSeconds must be shorter than the client timeout (timeoutMs)');
                }

                body['waitSeconds'] = options.waitSeconds;
            } else {
                const safeWaitSeconds = Math.max(0, Math.floor(getTimeoutMs() / 1000) - WAIT_HEADROOM_SECONDS);

                if (safeWaitSeconds < SERVER_DEFAULT_WAIT_SECONDS) {
                    body['waitSeconds'] = safeWaitSeconds;
                }
            }

            const path = `/workflows/${encodeURIComponent(workflowKey)}`;
            let run = expectObject<WorkflowRun>(
                await request({body, method: 'POST', path, signal: options.signal}),
                path
            );
            const delays = options.pollDelaysMs?.length ? options.pollDelaysMs : DEFAULT_POLL_DELAYS_MS;
            let attempt = 0;

            while (shouldKeepWaiting(run, options.wait ?? false)) {
                const delay = Math.max(MIN_POLL_DELAY_MS, delays[Math.min(attempt, delays.length - 1)] ?? 0);

                attempt += 1;

                try {
                    await sleep(delay, options.signal);

                    run = await getRun(run.runId, options);
                } catch (error) {
                    if (options.signal?.aborted) {
                        throw new BytechefAppError('ABORTED', `Stopped waiting for workflow run ${run.runId}`, {
                            cause: error,
                            runId: run.runId,
                        });
                    }

                    if (error instanceof BytechefAppError) {
                        throw new BytechefAppError(error.code, error.message, {
                            cause: error,
                            runId: run.runId,
                            status: error.status,
                        });
                    }

                    throw error;
                }
            }

            return run;
        },

        getRun,

        async list(options = {}) {
            const path = '/workflows';

            return expectArray<AppWorkflow>(await request({method: 'GET', path, signal: options.signal}), path);
        },
    };
}
