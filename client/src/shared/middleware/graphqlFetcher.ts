import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

const OPERATION_NAME_PATTERN = /^\s*(?:query|mutation)\s+(\w+)/;

export const USER_GRAPHQL_OPERATION_NAMES: ReadonlySet<string> = new Set([
    'authorities',
    'createIdentityProvider',
    'deleteIdentityProvider',
    'deleteUser',
    'identityProvider',
    'identityProviders',
    'inviteUser',
    'updateIdentityProvider',
    'updateUser',
    'users',
]);

function getSchemaHeaders(query: string): Record<string, string> {
    const operationName = OPERATION_NAME_PATTERN.exec(query)?.[1];

    if (operationName && USER_GRAPHQL_OPERATION_NAMES.has(operationName)) {
        return {'X-ByteChef-GraphQL-Schema': 'user'};
    }

    return {};
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
                ...getSchemaHeaders(query.toString()),
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
            },
        });

        if (!res.ok) {
            const errorJson = await res.json().catch(() => null);
            const serverMessage = errorJson?.errors?.[0]?.message;

            throw new Error(serverMessage || `GraphQL request failed with status ${res.status}`);
        }

        const json = await res.json();

        if (json.errors) {
            const {message} = json.errors[0];

            throw new Error(message);
        }

        return json.data;
    };
}
