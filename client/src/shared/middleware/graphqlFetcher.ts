import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

interface GraphQlResponseErrorBodyI {
    extensions?: {errorCode?: unknown};
    message?: string;
}

/** A GraphQL error response, carrying the server's {@code extensions.errorCode} when it sent one. */
export class GraphQlResponseError extends Error {
    readonly errorCode: string | undefined;

    constructor(message: string, errorCode?: string) {
        super(message);

        this.errorCode = errorCode;
        this.name = 'GraphQlResponseError';
    }
}

const toGraphQlResponseError = (error: GraphQlResponseErrorBodyI | undefined, fallbackMessage: string) => {
    const errorCode = error?.extensions?.errorCode;

    return new GraphQlResponseError(
        error?.message || fallbackMessage,
        typeof errorCode === 'string' ? errorCode : undefined
    );
};

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

            throw toGraphQlResponseError(errorJson?.errors?.[0], `GraphQL request failed with status ${res.status}`);
        }

        const json = await res.json();

        if (json.errors) {
            throw toGraphQlResponseError(json.errors[0], '');
        }

        return json.data;
    };
}
