import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

export class GraphQlRequestError extends Error {
    classification?: string;
    status?: number;

    constructor(message: string, {classification, status}: {classification?: string; status?: number} = {}) {
        super(message);

        this.classification = classification;
        this.name = 'GraphQlRequestError';
        this.status = status;
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

            throw new GraphQlRequestError(serverMessage || `GraphQL request failed with status ${res.status}`, {
                classification: errorJson?.errors?.[0]?.extensions?.classification,
                status: res.status,
            });
        }

        const json = await res.json();

        if (json.errors) {
            const {extensions, message} = json.errors[0];

            throw new GraphQlRequestError(message, {classification: extensions?.classification});
        }

        return json.data;
    };
}
