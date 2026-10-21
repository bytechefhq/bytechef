import {afterEach, describe, expect, it, vi} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createBytechefClient} from './client';

describe('createBytechefClient', () => {
    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it('reads the environment lazily, on the first request, not at construction', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: []}));
        const client = createBytechefClient({fetch});

        vi.stubEnv('BYTECHEF_URL', 'https://late.example');
        vi.stubEnv('BYTECHEF_APP_KEY', 'late-key');

        await client.connections.list();

        expect(lastRequest(calls).url).toBe('https://late.example/api/app/v1/connections');
        expect(lastRequest(calls).headers.get('authorization')).toBe('Bearer late-key');
    });

    it('checks waitSeconds against the timeoutMs the client was configured with', async () => {
        const run = {
            approvalFormUrl: null,
            error: null,
            result: undefined,
            runId: 'run-1',
            status: 'RUNNING',
            workflowKey: 'approve',
        };
        const {calls, fetch} = mockFetch(jsonResponse({body: run, status: 202}));
        const client = createBytechefClient({appKey: 'k', fetch, timeoutMs: 30_000, url: 'https://x'});

        await expect(client.workflows.execute('approve', {}, {waitSeconds: 30})).rejects.toBeInstanceOf(RangeError);
        expect(calls).toHaveLength(0);

        await expect(client.workflows.execute('approve', {}, {waitSeconds: 29})).resolves.toEqual(run);
        expect(calls).toHaveLength(1);
    });

    it('exposes the six namespaces', () => {
        const client = createBytechefClient({appKey: 'k', url: 'https://x'});

        expect(Object.keys(client).sort()).toEqual(['actions', 'api', 'connections', 'proxy', 'tools', 'workflows']);
    });
});
