import {afterEach, describe, expect, it} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {bytechefMiddleware, type BytechefContext} from './middleware';

function runServer(middleware: ReturnType<typeof bytechefMiddleware>): BytechefContext {
    const server = middleware.options.server;

    if (!server) {
        throw new Error('bytechefMiddleware has no server hook');
    }

    const result = server({next: (options: unknown) => options} as never) as unknown as {context: BytechefContext};

    return result.context;
}

describe('bytechefMiddleware', () => {
    afterEach(() => {
        delete (globalThis as {window?: unknown}).window;
    });

    it('puts a client with the six namespaces on context.bytechef', () => {
        const context = runServer(bytechefMiddleware({appKey: 'k', url: 'https://x'}));

        expect(Object.keys(context.bytechef).sort()).toEqual([
            'actions',
            'api',
            'connections',
            'proxy',
            'tools',
            'workflows',
        ]);
    });

    it('reuses one client across requests', () => {
        const middleware = bytechefMiddleware({appKey: 'k', url: 'https://x'});

        expect(runServer(middleware).bytechef).toBe(runServer(middleware).bytechef);
    });

    it('passes its options to the client', async () => {
        const {calls, fetch} = mockFetch(jsonResponse({body: []}));
        const context = runServer(bytechefMiddleware({appKey: 'k', fetch, url: 'https://opt.example'}));

        await context.bytechef.connections.list();

        expect(lastRequest(calls).url).toBe('https://opt.example/api/app/v1/connections');
    });

    it('does not create the client until the server hook runs', () => {
        (globalThis as {window?: unknown}).window = {};

        expect(() => bytechefMiddleware({appKey: 'k', url: 'https://x'})).not.toThrow();
    });
});
