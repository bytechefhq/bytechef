import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createRequester} from './request';
import {createWorkflowsApi, DEFAULT_POLL_DELAYS_MS, defaultSleep, isFinished} from './workflows';

const running = {
    approvalFormUrl: null,
    error: null,
    result: undefined,
    runId: 'run-1',
    status: 'RUNNING',
    workflowKey: 'approve',
};
const approval = {...running, approvalFormUrl: 'https://bc.example/resume/t', status: 'APPROVAL_REQUIRED'};
const completed = {...running, result: {approved: true}, status: 'COMPLETED'};
const failed = {...running, error: {message: 'boom'}, status: 'FAILED'};

function workflowsWith(...responses: Parameters<typeof mockFetch>) {
    const {calls, fetch} = mockFetch(...responses);
    const config = {appKey: 'k', baseUrl: 'https://bc.example', fetch, timeoutMs: 30_000};
    const request = createRequester(() => config);
    const sleepHooks: Array<() => void> = [];
    const sleeps: number[] = [];
    const sleep = async (ms: number, signal?: AbortSignal) => {
        sleeps.push(ms);
        sleepHooks.shift()?.();

        if (signal?.aborted) {
            throw signal.reason;
        }
    };

    return {calls, config, sleepHooks, sleeps, workflows: createWorkflowsApi(request, () => config.timeoutMs, sleep)};
}

describe('isFinished', () => {
    it.each([
        ['COMPLETED', true],
        ['FAILED', true],
        ['CANCELLED', true],
        ['RUNNING', false],
        ['APPROVAL_REQUIRED', false],
    ] as const)('%s → %s', (status, expected) => {
        expect(isFinished(status)).toBe(expected);
    });
});

describe('workflows.list', () => {
    it('GETs /workflows', async () => {
        const workflow = {
            available: true,
            description: null,
            inputSchema: {type: 'object'},
            name: 'Approve',
            outputSchema: null,
            workflowKey: 'approve',
        };
        const {calls, workflows} = workflowsWith(jsonResponse({body: [workflow]}));

        await expect(workflows.list()).resolves.toEqual([workflow]);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/workflows');
    });
});

describe('workflows.execute', () => {
    it('POSTs input and waitSeconds and returns the run as reported, 200 or 202', async () => {
        const {calls, workflows} = workflowsWith(jsonResponse({body: running, status: 202}));

        await expect(workflows.execute('approve', {orderId: 'A-1'}, {waitSeconds: 5})).resolves.toEqual(running);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/workflows/approve');
        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({input: {orderId: 'A-1'}, waitSeconds: 5});
    });

    it('rejects a waitSeconds the client timeout would cut off, without sending', async () => {
        const {calls, workflows} = workflowsWith(jsonResponse({body: running, status: 202}));

        await expect(workflows.execute('approve', {}, {waitSeconds: 30})).rejects.toThrow(
            new RangeError('waitSeconds must be shorter than the client timeout (timeoutMs)')
        );
        expect(calls).toHaveLength(0);
    });

    it('sends a waitSeconds shorter than the client timeout', async () => {
        const {calls, workflows} = workflowsWith(jsonResponse({body: running, status: 202}));

        await expect(workflows.execute('approve', {}, {waitSeconds: 29})).resolves.toEqual(running);
        expect(calls).toHaveLength(1);
        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({input: {}, waitSeconds: 29});
    });

    it('omits waitSeconds when not given and the server default hold fits the client timeout', async () => {
        const {calls, config, workflows} = workflowsWith(jsonResponse({body: completed}));

        config.timeoutMs = 35_000;

        await workflows.execute('approve', {});

        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({input: {}});
    });

    it.each([
        [34_999, 29],
        [30_000, 25],
        [10_500, 5],
        [5_000, 0],
        [1_000, 0],
    ])('caps an omitted waitSeconds below a client timeout of %i ms at %i', async (timeoutMs, expectedWaitSeconds) => {
        const {calls, config, workflows} = workflowsWith(jsonResponse({body: completed}));

        config.timeoutMs = timeoutMs;

        await workflows.execute('approve', {}, {wait: true});

        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({input: {}, waitSeconds: expectedWaitSeconds});
    });

    it('caps an omitted waitSeconds without wait too, since the server holds the start request either way', async () => {
        const {calls, config, workflows} = workflowsWith(jsonResponse({body: running, status: 202}));

        config.timeoutMs = 20_000;

        await workflows.execute('approve', {});

        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({input: {}, waitSeconds: 15});
    });

    it('returns a FAILED run as data', async () => {
        const {workflows} = workflowsWith(jsonResponse({body: failed}));

        await expect(workflows.execute('approve', {})).resolves.toEqual(failed);
    });

    it('with wait: true polls getRun with the backoff until finished', async () => {
        const {calls, sleeps, workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: running}),
            jsonResponse({body: running}),
            jsonResponse({body: completed})
        );

        await expect(workflows.execute('approve', {}, {wait: true})).resolves.toEqual(completed);
        expect(sleeps).toEqual([1_000, 2_000, 3_000]);
        expect(calls.slice(1).map((call) => lastRequest([call]).url)).toEqual(
            Array(3).fill('https://bc.example/api/app/v1/workflow-runs/run-1')
        );
    });

    it('caps the backoff at the last delay', async () => {
        const responses = [
            jsonResponse({body: running, status: 202}),
            ...Array(6).fill(jsonResponse({body: running})),
            jsonResponse({body: completed}),
        ];
        const {sleeps, workflows} = workflowsWith(...responses);

        await workflows.execute('approve', {}, {wait: true});

        expect(sleeps).toEqual([...DEFAULT_POLL_DELAYS_MS, 5_000, 5_000]);
    });

    it('with wait: true stops at APPROVAL_REQUIRED', async () => {
        const {sleeps, workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: approval})
        );

        await expect(workflows.execute('approve', {}, {wait: true})).resolves.toEqual(approval);
        expect(sleeps).toEqual([1_000]);
    });

    it("with wait: 'approval' keeps polling through APPROVAL_REQUIRED", async () => {
        const {workflows} = workflowsWith(
            jsonResponse({body: approval, status: 202}),
            jsonResponse({body: approval}),
            jsonResponse({body: completed})
        );

        await expect(workflows.execute('approve', {}, {wait: 'approval'})).resolves.toEqual(completed);
    });

    it('does not poll when the run already finished', async () => {
        const {calls, workflows} = workflowsWith(jsonResponse({body: completed}));

        await workflows.execute('approve', {}, {wait: true});

        expect(calls).toHaveLength(1);
    });

    it('aborts the wait with ABORTED carrying the runId', async () => {
        const controller = new AbortController();
        const {calls, sleepHooks, workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: running})
        );

        sleepHooks.push(() => controller.abort());

        await expect(workflows.execute('approve', {}, {signal: controller.signal, wait: true})).rejects.toMatchObject({
            code: 'ABORTED',
            runId: 'run-1',
        });
        expect(calls).toHaveLength(1);
    });

    it('aborts while a poll request is in flight with ABORTED carrying the runId', async () => {
        const controller = new AbortController();
        const pendingPoll = (_input: RequestInfo | URL, init?: RequestInit) =>
            new Promise<Response>((_resolve, reject) => {
                init!.signal!.addEventListener('abort', () => reject(init!.signal!.reason), {once: true});
                queueMicrotask(() => controller.abort());
            });
        const {calls, workflows} = workflowsWith(jsonResponse({body: running, status: 202}), pendingPoll);

        await expect(workflows.execute('approve', {}, {signal: controller.signal, wait: true})).rejects.toMatchObject({
            code: 'ABORTED',
            runId: 'run-1',
        });
        expect(calls).toHaveLength(2);
    });

    it('POSTs to a URL-encoded workflow key', async () => {
        const {calls, workflows} = workflowsWith(jsonResponse({body: completed}));

        await workflows.execute('a b/c', {});

        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/workflows/a%20b%2Fc');
    });

    it('propagates a server error answered with the ABORTED code instead of rewrapping it as a client abort', async () => {
        const {workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: {code: 'ABORTED', message: 'x'}, status: 409})
        );
        const error = await workflows.execute('approve', {}, {wait: true}).catch((caught: unknown) => caught);

        expect(error).toMatchObject({code: 'ABORTED', message: 'x', runId: 'run-1', status: 409});
        expect((error as {cause: unknown}).cause).toMatchObject({code: 'ABORTED', status: 409});
    });

    it('keeps the code, message and status of a failed poll and adds the runId', async () => {
        const {workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: {code: 'APP_WORKFLOW_RUN_NOT_FOUND', message: 'gone'}, status: 404})
        );

        await expect(workflows.execute('approve', {}, {wait: true})).rejects.toMatchObject({
            code: 'APP_WORKFLOW_RUN_NOT_FOUND',
            message: 'gone',
            runId: 'run-1',
            status: 404,
        });
    });

    it('adds the runId to a network failure while polling', async () => {
        const {workflows} = workflowsWith(jsonResponse({body: running, status: 202}), new TypeError('fetch failed'));

        await expect(workflows.execute('approve', {}, {wait: true})).rejects.toMatchObject({
            code: 'NETWORK_ERROR',
            runId: 'run-1',
        });
    });

    it('adds the runId to a TIMEOUT while polling and keeps the timeout as the cause', async () => {
        const {fetch} = mockFetch(
            jsonResponse({body: running, status: 202}),
            (_input, init) =>
                new Promise<Response>((_resolve, reject) => {
                    init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                })
        );
        const config = {appKey: 'k', baseUrl: 'https://bc.example', fetch, timeoutMs: 50};
        const workflows = createWorkflowsApi(
            createRequester(() => config),
            () => config.timeoutMs,
            async () => undefined
        );

        const error = await workflows.execute('approve', {}, {wait: true}).catch((caught: unknown) => caught);

        expect(error).toMatchObject({code: 'TIMEOUT', runId: 'run-1', status: undefined});
        expect((error as {cause: unknown}).cause).toMatchObject({code: 'TIMEOUT'});
    });

    it('propagates an error that is not a BytechefAppError unchanged', async () => {
        const failure = new RangeError('sleep broke');
        const {sleepHooks, workflows} = workflowsWith(jsonResponse({body: running, status: 202}));

        sleepHooks.push(() => {
            throw failure;
        });

        await expect(workflows.execute('approve', {}, {wait: true})).rejects.toBe(failure);
    });

    it('honours pollDelaysMs and repeats the last delay', async () => {
        const {sleeps, workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: running}),
            jsonResponse({body: completed})
        );

        await workflows.execute('approve', {}, {pollDelaysMs: [250], wait: true});

        expect(sleeps).toEqual([250, 250]);
    });

    it('falls back to the default delays when pollDelaysMs is empty', async () => {
        const {sleeps, workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: completed})
        );

        await workflows.execute('approve', {}, {pollDelaysMs: [], wait: true});

        expect(sleeps).toEqual([1_000]);
    });

    it('clamps a poll delay to at least 100 ms', async () => {
        const {sleeps, workflows} = workflowsWith(
            jsonResponse({body: running, status: 202}),
            jsonResponse({body: completed})
        );

        await workflows.execute('approve', {}, {pollDelaysMs: [0], wait: true});

        expect(sleeps).toEqual([100]);
    });
});

describe('workflows.getRun', () => {
    it('GETs the run and URL-encodes the id', async () => {
        const {calls, workflows} = workflowsWith(jsonResponse({body: completed}));

        await expect(workflows.getRun('run 1')).resolves.toEqual(completed);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/workflow-runs/run%201');
    });

    it('throws the server 404 for an unknown run', async () => {
        const {workflows} = workflowsWith(
            jsonResponse({body: {code: 'APP_WORKFLOW_RUN_NOT_FOUND', message: 'no'}, status: 404})
        );

        await expect(workflows.getRun('x')).rejects.toMatchObject({code: 'APP_WORKFLOW_RUN_NOT_FOUND', status: 404});
    });
});

describe('defaultSleep', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('resolves after the delay and leaves no timer behind', async () => {
        const sleeping = defaultSleep(1_000);

        await vi.advanceTimersByTimeAsync(1_000);
        await sleeping;

        expect(vi.getTimerCount()).toBe(0);
    });

    it('rejects at once when aborted during the delay and leaves no timer behind', async () => {
        const controller = new AbortController();
        const reason = new Error('stop');
        const sleeping = defaultSleep(1_000, controller.signal);
        const settled = expect(sleeping).rejects.toBe(reason);

        controller.abort(reason);

        await settled;

        expect(vi.getTimerCount()).toBe(0);
    });

    it('rejects an already-aborted signal without arming a timer', async () => {
        const controller = new AbortController();

        controller.abort();

        const sleeping = defaultSleep(1_000, controller.signal);

        sleeping.catch(() => undefined);

        expect(vi.getTimerCount()).toBe(0);

        await expect(sleeping).rejects.toBe(controller.signal.reason);
    });
});
