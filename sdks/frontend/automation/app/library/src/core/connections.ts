import type {Requester} from './request';
import {expectArray, expectObject} from './response';
import type {AppAction, AppBinding} from './types';

export interface CallOptions {
    signal?: AbortSignal;
}

export interface ExecuteActionOptions extends CallOptions {
    componentVersion?: number;
}

export function createConnectionsApi(request: Requester) {
    return {
        async list(options: CallOptions = {}): Promise<AppBinding[]> {
            const path = '/connections';

            return expectArray<AppBinding>(await request({method: 'GET', path, signal: options.signal}), path);
        },
    };
}

export function createActionsApi(request: Requester) {
    return {
        async execute<T = unknown>(
            bindingKey: string,
            actionName: string,
            input: Record<string, unknown>,
            options: ExecuteActionOptions = {}
        ): Promise<T> {
            const body: Record<string, unknown> = {input};

            if (options.componentVersion !== undefined) {
                body['componentVersion'] = options.componentVersion;
            }

            const path = `/connections/${encodeURIComponent(bindingKey)}/actions/${encodeURIComponent(actionName)}`;
            const data = expectObject<{result: T}>(
                await request({body, method: 'POST', path, signal: options.signal}),
                path
            );

            return data.result;
        },

        async list(options: CallOptions = {}): Promise<AppAction[]> {
            const path = '/actions';

            return expectArray<AppAction>(await request({method: 'GET', path, signal: options.signal}), path);
        },
    };
}
