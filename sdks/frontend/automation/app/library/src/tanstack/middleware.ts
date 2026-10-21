import {createMiddleware} from '@tanstack/react-start';

import {createBytechefClient, type BytechefClient, type BytechefClientOptions} from '../index';

export interface BytechefContext {
    bytechef: BytechefClient;
}

export function bytechefMiddleware(options: BytechefClientOptions = {}) {
    let client: BytechefClient | undefined;

    return createMiddleware({type: 'function'}).server(({next}) => {
        client ??= createBytechefClient(options);

        return next({context: {bytechef: client}});
    });
}

/**
 * The middleware of the ready-made server functions. It keeps the client a request middleware such as
 * `bytechefAuthMiddleware()` already put on `context.bytechef`, which carries the signed-in user's token, and creates
 * the key-only client from the environment only when there is none.
 */
export function serverFunctionMiddleware() {
    let client: BytechefClient | undefined;

    return createMiddleware({type: 'function'}).server(({context, next}) => {
        const requestClient = (context as Partial<BytechefContext> | undefined)?.bytechef;

        if (requestClient !== undefined) {
            return next({context: {bytechef: requestClient}});
        }

        client ??= createBytechefClient();

        return next({context: {bytechef: client}});
    });
}
