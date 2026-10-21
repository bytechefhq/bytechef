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
