import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

export type GraphQLErrorExtensionsType = Record<string, unknown>;

export class GraphQLResponseError extends Error {
    readonly extensions?: GraphQLErrorExtensionsType;

    constructor(message: string, extensions?: GraphQLErrorExtensionsType) {
        super(message);

        this.extensions = extensions;
        this.name = 'GraphQLResponseError';
    }
}

export function fetcher<TData, TVariables>(
    query: string | {toString(): string},
    variables?: TVariables,
) {
    return async (): Promise<TData> => {
        const res = await fetch(endpointUrl as string, {
            method: 'POST',
            ...fetchParams,
            body: JSON.stringify({query: query.toString(), variables}),
            headers: {
                ...fetchParams.headers,
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
            },
        });

        if (!res.ok) {
            const errorJson = await res.json().catch(() => null);
            const serverError = errorJson?.errors?.[0];

            if (serverError?.message) {
                throw new GraphQLResponseError(serverError.message, serverError.extensions);
            }

            throw new Error(`GraphQL request failed with status ${res.status}`);
        }

        const json = await res.json();

        if (json.errors) {
            const {extensions, message} = json.errors[0];

            throw new GraphQLResponseError(message, extensions);
        }

        return json.data;
    };
}
