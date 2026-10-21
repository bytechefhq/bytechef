import {describe, expect, it} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createActionsApi, createConnectionsApi} from './connections';
import {createRequester, type Requester} from './request';

const binding = {
    actionNames: ['createRecord'],
    bindingKey: 'crm',
    componentName: 'hubspot',
    componentVersion: 3,
    connectionVersion: 1,
    proxyAllowedMethods: ['GET'],
    proxyEnabled: true,
    toolNames: [],
};

function requesterWith(...responses: Parameters<typeof mockFetch>) {
    const {calls, fetch} = mockFetch(...responses);

    return {
        calls,
        request: createRequester(() => ({appKey: 'k', baseUrl: 'https://bc.example', fetch, timeoutMs: 1000})),
    };
}

describe('connections.list', () => {
    it('GETs /connections and returns the bindings', async () => {
        const {calls, request} = requesterWith(jsonResponse({body: [binding]}));

        await expect(createConnectionsApi(request).list()).resolves.toEqual([binding]);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/connections');
    });
});

describe('actions.execute', () => {
    it('POSTs the input and unwraps result', async () => {
        const {calls, request} = requesterWith(jsonResponse({body: {result: {id: '001'}}}));

        await expect(createActionsApi(request).execute('crm', 'createRecord', {name: 'ACME'})).resolves.toEqual({
            id: '001',
        });

        const sent = lastRequest(calls);

        expect(sent.url).toBe('https://bc.example/api/app/v1/connections/crm/actions/createRecord');
        expect(JSON.parse(sent.body as string)).toEqual({input: {name: 'ACME'}});
    });

    it('passes componentVersion when given', async () => {
        const {calls, request} = requesterWith(jsonResponse({body: {result: null}}));

        await createActionsApi(request).execute('crm', 'createRecord', {}, {componentVersion: 2});

        expect(JSON.parse(lastRequest(calls).body as string)).toEqual({componentVersion: 2, input: {}});
    });

    it('URL-encodes binding keys and action names', async () => {
        const {calls, request} = requesterWith(jsonResponse({body: {result: null}}));

        await createActionsApi(request).execute('a b', 'c/d', {});

        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/connections/a%20b/actions/c%2Fd');
    });
});

describe('actions.list', () => {
    it('GETs /actions and returns the actions with object schemas', async () => {
        const action = {
            actionName: 'createContact',
            bindingKey: 'crm',
            componentName: 'hubspot',
            description: null,
            inputSchema: {properties: {email: {type: 'string'}}, type: 'object'},
        };
        const {calls, request} = requesterWith(jsonResponse({body: [action]}));

        await expect(createActionsApi(request).list()).resolves.toEqual([action]);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/actions');
    });

    it('rejects with ABORTED and no request when the signal is already aborted', async () => {
        const controller = new AbortController();
        const {calls, request} = requesterWith(jsonResponse({body: []}));

        controller.abort();

        await expect(createActionsApi(request).list({signal: controller.signal})).rejects.toMatchObject({
            code: 'ABORTED',
        });
        expect(calls).toHaveLength(0);
    });
});

describe('signal forwarding', () => {
    it.each([
        ['connections.list', (request: Requester, signal: AbortSignal) => createConnectionsApi(request).list({signal})],
        [
            'actions.execute',
            (request: Requester, signal: AbortSignal) =>
                createActionsApi(request).execute('crm', 'createRecord', {}, {signal}),
        ],
    ])('%s rejects with ABORTED and sends nothing for an already-aborted signal', async (_name, call) => {
        const {calls, request} = requesterWith(jsonResponse({body: {result: null}}));
        const controller = new AbortController();

        controller.abort();

        await expect(call(request, controller.signal)).rejects.toMatchObject({code: 'ABORTED'});
        expect(calls).toHaveLength(0);
    });
});
