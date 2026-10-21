import {afterEach, describe, expect, it} from 'vitest';

import {jsonResponse, lastRequest, mockFetch} from '../test/mockFetch';
import {bytechefMiddleware, type BytechefContext, serverFunctionMiddleware} from './middleware';

function runServer(
    middleware: ReturnType<typeof bytechefMiddleware>,
    context?: Partial<BytechefContext>
): BytechefContext {
    const server = middleware.options.server;

    if (!server) {
        throw new Error('bytechefMiddleware has no server hook');
    }

    const result = server({context, next: (options: unknown) => options} as never) as unknown as {
        context: BytechefContext;
    };

    return result.context;
}

describe('bytechefMiddleware', () => {
    afterEach(() => {
        delete (globalThis as {window?: unknown}).window;
    });

    it('puts a client with the eight namespaces on context.bytechef', () => {
        const context = runServer(bytechefMiddleware({appKey: 'k', url: 'https://x'}));

        expect(Object.keys(context.bytechef).sort()).toEqual([
            'actions',
            'api',
            'auth',
            'connections',
            'context',
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

describe('serverFunctionMiddleware', () => {
    it('keeps the client the auth middleware put on context.bytechef, with the user token', () => {
        const userClient = runServer(bytechefMiddleware({appKey: 'k', url: 'https://x'})).bytechef;

        expect(runServer(serverFunctionMiddleware(), {bytechef: userClient}).bytechef).toBe(userClient);
    });

    it('creates a key-only client when no request middleware set one', () => {
        const context = runServer(serverFunctionMiddleware());

        expect(Object.keys(context.bytechef)).toContain('actions');
    });
});
