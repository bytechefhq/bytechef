import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

interface GraphQLResponseErrorBodyI {
    extensions?: {errorCode?: unknown};
    message?: string;
}

export class GraphQLResponseError extends Error {
    readonly errorCode: string | undefined;

    constructor(message: string, errorCode?: string) {
        super(message);

        this.errorCode = errorCode;
        this.name = 'GraphQLResponseError';
    }
}

export class GraphQLHttpError extends Error {
    readonly status: number;

    constructor(status: number) {
        super(`GraphQL request failed with status ${status}`);

        this.name = 'GraphQLHttpError';
        this.status = status;
    }
}

const toGraphQLResponseError = (error: GraphQLResponseErrorBodyI) => {
    const errorCode = error.extensions?.errorCode;

    return new GraphQLResponseError(error.message ?? '', typeof errorCode === 'string' ? errorCode : undefined);
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

            const serverError: GraphQLResponseErrorBodyI | undefined = errorJson?.errors?.[0];

            if (serverError?.message) {
                throw toGraphQLResponseError(serverError);
            }

            throw new GraphQLHttpError(res.status);
        }

        const json = await res.json();

        if (json.errors) {
            throw toGraphQLResponseError(json.errors[0]);
        }

        return json.data;
    };
}
