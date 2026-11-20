import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

export class GraphQLResponseError extends Error {
    constructor(message: string) {
        super(message);

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
            const serverMessage = errorJson?.errors?.[0]?.message;

            if (serverMessage) {
                throw new GraphQLResponseError(serverMessage);
            }

            throw new Error(`GraphQL request failed with status ${res.status}`);
        }

        const json = await res.json();

        if (json.errors) {
            const {message} = json.errors[0];

            throw new GraphQLResponseError(message);
        }

        return json.data;
    };
}
