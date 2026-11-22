import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

interface GraphQlRequestErrorOptionsI {
    classification?: string;
    status?: number;
}

export class GraphQlRequestError extends Error {
    classification?: string;
    status?: number;

    constructor(message: string, {classification, status}: GraphQlRequestErrorOptionsI = {}) {
        super(message);

        this.classification = classification;
        this.name = 'GraphQlRequestError';
        this.status = status;
    }
}

export class GraphQLResponseError extends GraphQlRequestError {
    constructor(message: string, options: GraphQlRequestErrorOptionsI = {}) {
        super(message, options);

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
            const classification = errorJson?.errors?.[0]?.extensions?.classification;

            if (serverMessage) {
                throw new GraphQLResponseError(serverMessage, {classification, status: res.status});
            }

            throw new GraphQlRequestError(`GraphQL request failed with status ${res.status}`, {
                classification,
                status: res.status,
            });
        }

        const json = await res.json();

        if (json.errors) {
            const {extensions, message} = json.errors[0];

            throw new GraphQLResponseError(message, {classification: extensions?.classification});
        }

        return json.data;
    };
}
