import {getCookie} from '@/shared/util/cookie-utils';

import {endpointUrl, fetchParams} from './config';

const IGNORED_TOKEN_PATTERN = /"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|#[^\n]*/g;

const OPERATION_HEADER_PATTERN = /^\s*(?:query|mutation|subscription)\s+(\w+)/;

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

export function getOperationNames(document: string): string[] {
    const strippedDocument = document.replace(IGNORED_TOKEN_PATTERN, ' ');
    const operationNames: string[] = [];

    let braceDepth = 0;
    let definitionStart = 0;
    let parenthesisDepth = 0;

    for (let index = 0; index < strippedDocument.length; index++) {
        const character = strippedDocument[index];

        if (character === '(') {
            parenthesisDepth++;
        } else if (character === ')') {
            parenthesisDepth--;
        } else if (parenthesisDepth > 0) {
            continue;
        } else if (character === '{') {
            if (braceDepth === 0) {
                const operationName = OPERATION_HEADER_PATTERN.exec(strippedDocument.slice(definitionStart, index))?.[1];

                if (operationName) {
                    operationNames.push(operationName);
                }
            }

            braceDepth++;
        } else if (character === '}') {
            braceDepth--;

            if (braceDepth === 0) {
                definitionStart = index + 1;
            }
        }
    }

    return operationNames;
}

function getSchemaHeaders(query: string): Record<string, string> {
    const operationNames = getOperationNames(query);

    if (
        operationNames.length > 0 &&
        operationNames.every((operationName) => USER_GRAPHQL_OPERATION_NAMES.has(operationName))
    ) {
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
