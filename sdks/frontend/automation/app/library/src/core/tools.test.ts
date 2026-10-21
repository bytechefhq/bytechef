import {describe, expect, it, vi} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {createRequester, type Requester} from './request';
import {createToolsApi, TOOL_LISTING_TTL_MS} from './tools';

const searchTool = {
    bindingKey: 'crm',
    componentName: 'hubspot',
    description: 'Search',
    name: 'CRM__SEARCH_RECORDS',
    parameters: '{"type":"object","properties":{"q":{"type":"string"}}}',
    toolName: 'searchRecords',
};

function toolsWith(now: () => number, ...responses: Parameters<typeof mockFetch>) {
    const {calls, fetch} = mockFetch(...responses);
    const request = createRequester(() => ({appKey: 'k', baseUrl: 'https://bc.example', fetch, timeoutMs: 1000}));

    return {calls, tools: createToolsApi(request, now)};
}

describe('tools.list', () => {
    it('parses parameters into an object', async () => {
        const {tools} = toolsWith(() => 0, jsonResponse({body: [searchTool]}));

        const [tool] = await tools.list();

        expect(tool!.parameters).toEqual({properties: {q: {type: 'string'}}, type: 'object'});
        expect(tool!.name).toBe('CRM__SEARCH_RECORDS');
    });

    it('lists an open object schema when parameters are not a JSON object', async () => {
        const {tools} = toolsWith(
            () => 0,
            jsonResponse({
                body: [
                    {...searchTool, parameters: 'not json'},
                    {...searchTool, name: 'CRM__OTHER', parameters: '"text"'},
                ],
            })
        );

        const listed = await tools.list();

        expect(listed.map((tool) => tool.parameters)).toEqual([
            {additionalProperties: true, type: 'object'},
            {additionalProperties: true, type: 'object'},
        ]);
    });
});

describe('tools.execute', () => {
    it('POSTs to the binding and tool and unwraps result', async () => {
        const {calls, tools} = toolsWith(() => 0, jsonResponse({body: {result: [1]}}));

        await expect(tools.execute('crm', 'searchRecords', {q: 'x'})).resolves.toEqual([1]);
        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/connections/crm/tools/searchRecords');
    });

    it('URL-encodes binding keys and tool names', async () => {
        const {calls, tools} = toolsWith(() => 0, jsonResponse({body: {result: null}}));

        await tools.execute('a b', 'c/d', {});

        expect(lastRequest(calls).url).toBe('https://bc.example/api/app/v1/connections/a%20b/tools/c%2Fd');
    });
});

describe('tools.executeByName', () => {
    it('maps the flat name through the listing and caches it', async () => {
        let clock = 0;
        const {calls, tools} = toolsWith(
            () => clock,
            jsonResponse({body: [searchTool]}),
            jsonResponse({body: {result: 'a'}}),
            jsonResponse({body: {result: 'b'}})
        );

        await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('a');
        clock = TOOL_LISTING_TTL_MS - 1;
        await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('b');

        expect(calls.map((call) => lastRequest([call]).url)).toEqual([
            'https://bc.example/api/app/v1/tools',
            'https://bc.example/api/app/v1/connections/crm/tools/searchRecords',
            'https://bc.example/api/app/v1/connections/crm/tools/searchRecords',
        ]);
    });

    it('re-fetches once on a miss and then executes the newly allowed tool', async () => {
        const {calls, tools} = toolsWith(
            () => 0,
            jsonResponse({body: []}),
            jsonResponse({body: [searchTool]}),
            jsonResponse({body: {result: 'ok'}})
        );

        await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('ok');
        expect(calls).toHaveLength(3);
    });

    it('throws APP_TOOL_NOT_FOUND after exactly one re-fetch when the tool is gone', async () => {
        const {calls, tools} = toolsWith(() => 0, jsonResponse({body: []}), jsonResponse({body: []}));

        await expect(tools.executeByName('CRM__GONE', {})).rejects.toMatchObject({code: 'APP_TOOL_NOT_FOUND'});
        expect(calls).toHaveLength(2);
    });

    it('refreshes the listing after the TTL', async () => {
        let clock = 0;
        const {calls, tools} = toolsWith(
            () => clock,
            jsonResponse({body: [searchTool]}),
            jsonResponse({body: {result: 1}}),
            jsonResponse({body: [searchTool]}),
            jsonResponse({body: {result: 2}})
        );

        await tools.executeByName('CRM__SEARCH_RECORDS', {});
        clock = TOOL_LISTING_TTL_MS + 1;
        await tools.executeByName('CRM__SEARCH_RECORDS', {});

        expect(calls.filter((call) => lastRequest([call]).url.endsWith('/tools'))).toHaveLength(2);
    });
});

describe('listing isolation', () => {
    it('does not let callers mutate the cached listing through the array list() returned', async () => {
        const {tools} = toolsWith(
            () => 0,
            jsonResponse({body: [searchTool]}),
            jsonResponse({body: []}),
            jsonResponse({body: []})
        );

        const listed = await tools.list();

        listed.push({...searchTool, name: 'CRM__FAKE', parameters: {}});

        await expect(tools.executeByName('CRM__FAKE', {})).rejects.toMatchObject({code: 'APP_TOOL_NOT_FOUND'});
    });
});

describe('signal forwarding', () => {
    it.each([
        ['tools.list', (api: ReturnType<typeof createToolsApi>, signal: AbortSignal) => api.list({signal})],
        [
            'tools.execute',
            (api: ReturnType<typeof createToolsApi>, signal: AbortSignal) =>
                api.execute('crm', 'searchRecords', {}, {signal}),
        ],
        [
            'tools.executeByName',
            (api: ReturnType<typeof createToolsApi>, signal: AbortSignal) =>
                api.executeByName('CRM__SEARCH_RECORDS', {}, {signal}),
        ],
    ])('%s rejects with ABORTED and sends nothing for an already-aborted signal', async (_name, call) => {
        const {fetch, calls} = mockFetch(jsonResponse({body: [searchTool]}));
        const request: Requester = createRequester(() => ({
            appKey: 'k',
            baseUrl: 'https://bc.example',
            fetch,
            timeoutMs: 1000,
        }));
        const controller = new AbortController();

        controller.abort();

        await expect(
            call(
                createToolsApi(request, () => 0),
                controller.signal
            )
        ).rejects.toMatchObject({
            code: 'ABORTED',
        });
        expect(calls).toHaveLength(0);
    });
});

describe('executeByName signal forwarding', () => {
    it('forwards the signal to the execution when the cached listing answers', async () => {
        const {calls, tools} = toolsWith(
            () => 0,
            jsonResponse({body: [searchTool]}),
            jsonResponse({body: {result: 1}})
        );
        const controller = new AbortController();

        await tools.list();

        controller.abort();

        await expect(tools.executeByName('CRM__SEARCH_RECORDS', {}, {signal: controller.signal})).rejects.toMatchObject(
            {code: 'ABORTED'}
        );
        expect(calls).toHaveLength(1);
    });
});

describe('executeByName shared listing', () => {
    it('shares one listing request between concurrent cold calls', async () => {
        let listingRequests = 0;
        const {calls, tools} = toolsWith(
            () => 0,
            (input) => {
                if (String(input).endsWith('/tools')) {
                    listingRequests += 1;

                    return jsonResponse({body: [searchTool]});
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );

        await expect(
            Promise.all([
                tools.executeByName('CRM__SEARCH_RECORDS', {}),
                tools.executeByName('CRM__SEARCH_RECORDS', {}),
            ])
        ).resolves.toEqual(['ok', 'ok']);
        expect(listingRequests).toBe(1);
        expect(calls).toHaveLength(3);
    });

    it('lets one caller abort without aborting the listing request the others wait on', async () => {
        let listingSignal: AbortSignal | undefined;
        let releaseListing: () => void = () => undefined;
        const {tools} = toolsWith(
            () => 0,
            (input, init) => {
                if (String(input).endsWith('/tools')) {
                    listingSignal = init?.signal ?? undefined;

                    return new Promise<Response>((resolve, reject) => {
                        init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                        releaseListing = () => resolve(jsonResponse({body: [searchTool]}));
                    });
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );
        const leaving = new AbortController();
        const leavingCall = tools.executeByName('CRM__SEARCH_RECORDS', {}, {signal: leaving.signal});
        const stayingCall = tools.executeByName('CRM__SEARCH_RECORDS', {});

        await vi.waitFor(() => expect(listingSignal).toBeDefined());

        leaving.abort('user left');

        await expect(leavingCall).rejects.toMatchObject({cause: 'user left', code: 'ABORTED'});
        expect(listingSignal!.aborted).toBe(false);

        releaseListing();

        await expect(stayingCall).resolves.toBe('ok');
    });

    it('still fills the cache when every caller left and the abandoned listing then succeeds', async () => {
        let listingRequests = 0;
        let releaseListing: () => void = () => undefined;
        const {tools} = toolsWith(
            () => 0,
            (input, init) => {
                if (String(input).endsWith('/tools')) {
                    listingRequests += 1;

                    if (listingRequests === 1) {
                        return new Promise<Response>((resolve, reject) => {
                            init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                            releaseListing = () => resolve(jsonResponse({body: [searchTool]}));
                        });
                    }

                    return jsonResponse({body: [searchTool]});
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );
        const leaving = new AbortController();
        const leavingCall = tools.executeByName('CRM__SEARCH_RECORDS', {}, {signal: leaving.signal});

        await vi.waitFor(() => expect(listingRequests).toBe(1));

        leaving.abort();

        await expect(leavingCall).rejects.toMatchObject({code: 'ABORTED'});

        releaseListing();

        await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('ok');
        expect(listingRequests).toBe(1);
    });

    it('hands a failed listing to every waiting caller and fetches afresh on the next call', async () => {
        let listingRequests = 0;
        const {tools} = toolsWith(
            () => 0,
            (input) => {
                if (String(input).endsWith('/tools')) {
                    listingRequests += 1;

                    return listingRequests === 1
                        ? jsonResponse({body: {code: 'BOOM', message: 'down'}, status: 503})
                        : jsonResponse({body: [searchTool]});
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );

        const settled = await Promise.allSettled([
            tools.executeByName('CRM__SEARCH_RECORDS', {}),
            tools.executeByName('CRM__SEARCH_RECORDS', {}),
        ]);

        expect(
            settled.map((outcome) => (outcome.status === 'rejected' ? (outcome.reason as {code: string}).code : 'ok'))
        ).toEqual(['BOOM', 'BOOM']);

        await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('ok');
        expect(listingRequests).toBe(2);
    });

    it('leaves no unhandled rejection when every caller left and the abandoned listing then fails', async () => {
        let listingRequests = 0;
        let failListing: () => void = () => undefined;
        const {tools} = toolsWith(
            () => 0,
            (input, init) => {
                if (String(input).endsWith('/tools')) {
                    listingRequests += 1;

                    if (listingRequests === 1) {
                        return new Promise<Response>((resolve, reject) => {
                            init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                            failListing = () =>
                                resolve(jsonResponse({body: {code: 'BOOM', message: 'down'}, status: 503}));
                        });
                    }

                    return jsonResponse({body: [searchTool]});
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );
        const leaving = new AbortController();
        const leavingCall = tools.executeByName('CRM__SEARCH_RECORDS', {}, {signal: leaving.signal});

        leaving.abort();

        await expect(leavingCall).rejects.toMatchObject({code: 'ABORTED'});

        failListing();

        await vi.waitFor(async () => {
            await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('ok');
        });
        expect(listingRequests).toBe(2);
    });
});
