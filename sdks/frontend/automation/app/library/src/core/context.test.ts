import {afterEach, describe, expect, it, vi} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createBytechefClient} from './client';
import type {AppContext} from './types';

const CONTEXT: AppContext = {
    appId: 1,
    appInstanceId: 2,
    appInstanceName: 'Production',
    appName: 'CRM',
    authorizationEndpoint: 'https://bc.example/oauth2/authorize',
    clientId: 'app-1',
    configuration: {region: 'eu'},
    environment: 'PRODUCTION',
    issuer: 'https://bc.example',
    tokenEndpoint: 'https://bc.example/oauth2/token',
    userAuthMode: 'REQUIRED',
};

describe('context', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it('context.get is cached per client', async () => {
        const first = mockFetch(jsonResponse({body: CONTEXT}));
        const second = mockFetch(jsonResponse({body: {...CONTEXT, appInstanceId: 3}}));
        const firstClient = createBytechefClient({appKey: 'key-1', fetch: first.fetch, url: 'https://bc.example'});
        const secondClient = createBytechefClient({appKey: 'key-2', fetch: second.fetch, url: 'https://bc.example'});

        await expect(firstClient.context.get()).resolves.toEqual(CONTEXT);
        await expect(firstClient.context.get()).resolves.toEqual(CONTEXT);
        await expect(secondClient.context.get()).resolves.toMatchObject({appInstanceId: 3});

        expect(first.calls).toHaveLength(1);
        expect(second.calls).toHaveLength(1);
        expect(lastRequest(first.calls).url).toBe('https://bc.example/api/app/v1/context');
        expect(lastRequest(first.calls).headers.get('authorization')).toBe('Bearer key-1');
        expect(lastRequest(second.calls).headers.get('authorization')).toBe('Bearer key-2');
    });

    it('shares one request between concurrent callers', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: CONTEXT}));
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await Promise.all([client.context.get(), client.context.get()]);

        expect(calls).toHaveLength(1);
    });

    it('does not cache a failure', async () => {
        const {calls, fetch} = mockFetch(
            jsonResponse({body: {code: 'APP_KEY_INVALID', message: 'bad key'}, status: 401}),
            jsonResponse({body: CONTEXT})
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(client.context.get()).rejects.toMatchObject({code: 'APP_KEY_INVALID', status: 401});
        await expect(client.context.get()).resolves.toEqual(CONTEXT);

        expect(calls).toHaveLength(2);
    });

    it('rejects an answer that is not an object', async () => {
        const {fetch} = mockFetch(jsonResponse({body: []}));
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await expect(client.context.get()).rejects.toMatchObject({code: 'INVALID_RESPONSE'});
    });

    it('refetches the context once the time-to-live has passed', async () => {
        vi.useFakeTimers();

        const {calls, fetch} = mockFetch(
            jsonResponse({body: CONTEXT}),
            jsonResponse({body: {...CONTEXT, clientId: 'app-2'}})
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await client.context.get();
        vi.advanceTimersByTime(59_999);
        await expect(client.context.get()).resolves.toMatchObject({clientId: 'app-1'});
        expect(calls).toHaveLength(1);

        vi.advanceTimersByTime(1);
        await expect(client.context.get()).resolves.toMatchObject({clientId: 'app-2'});
        expect(calls).toHaveLength(2);
    });

    it('takes the time-to-live from contextTtlMs', async () => {
        vi.useFakeTimers();

        const {calls, fetch} = mockFetch(jsonResponse({body: CONTEXT}));
        const client = createBytechefClient({appKey: 'key-1', contextTtlMs: 1_000, fetch, url: 'https://bc.example'});

        await client.context.get();
        vi.advanceTimersByTime(999);
        await client.context.get();
        expect(calls).toHaveLength(1);

        vi.advanceTimersByTime(1);
        await client.context.get();
        expect(calls).toHaveLength(2);
    });

    it('refuses a negative contextTtlMs', async () => {
        const {fetch} = mockFetch(jsonResponse({body: CONTEXT}));
        const client = createBytechefClient({appKey: 'key-1', contextTtlMs: -1, fetch, url: 'https://bc.example'});

        await expect(client.context.get()).rejects.toThrowError(/contextTtlMs/);
    });

    it('invalidate forces a refetch', async () => {
        const {calls, fetch} = mockFetch(
            jsonResponse({body: CONTEXT}),
            jsonResponse({body: {...CONTEXT, appName: 'CRM 2'}})
        );
        const client = createBytechefClient({appKey: 'key-1', fetch, url: 'https://bc.example'});

        await client.context.get();
        client.context.invalidate();

        await expect(client.context.get()).resolves.toMatchObject({appName: 'CRM 2'});
        expect(calls).toHaveLength(2);
    });
});
