import {McpServerCatalogQuery} from '@/shared/middleware/graphql';

import {ConnectionPrefillI, ConnectionPrefillOptionI} from '../types';

export type McpServerCatalogEntryType = McpServerCatalogQuery['mcpServerCatalog'][number];

export const MCP_CLIENT_COMPONENT_NAME = 'mcpClient';

const TRANSPORT_TYPES = ['httpStreamable', 'sse'];

const toKebabCase = (value: string) => value.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase();

const isAuthorizationHeader = (headerName: string) => headerName.toLowerCase() === 'authorization';

export function getMcpServerCatalogPrefill(entry: McpServerCatalogEntryType): ConnectionPrefillI {
    const {authentication} = entry;

    const parameters: Record<string, string> = {url: entry.url};

    if (TRANSPORT_TYPES.includes(entry.transportType)) {
        parameters.transportType = entry.transportType;
    }

    if (authentication.type === 'oauth2') {
        if (authentication.dynamicClientRegistration) {
            return {authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE', parameters};
        }

        if (authentication.authorizationUrl) {
            parameters.authorizationUrl = authentication.authorizationUrl;
        }

        if (authentication.tokenUrl) {
            parameters.tokenUrl = authentication.tokenUrl;
        }

        if (authentication.scopes?.length) {
            parameters.scopes = authentication.scopes.join(',');
        }

        return {
            authorizationType: authentication.pkce ? 'OAUTH2_AUTHORIZATION_CODE_PKCE' : 'OAUTH2_AUTHORIZATION_CODE',
            parameters,
        };
    }

    if (authentication.type === 'header') {
        const headers = authentication.headers ?? [];

        if (headers.length > 0 && headers.every((header) => isAuthorizationHeader(header.name))) {
            return {authorizationType: 'BEARER_TOKEN', parameters};
        }
    }

    return {authorizationType: undefined, parameters};
}

function getMcpServerCatalogNotes(entry: McpServerCatalogEntryType): string[] {
    const {authentication} = entry;

    const notes: string[] = [];

    if (entry.urlParameters?.length) {
        notes.push(
            `Replace ${entry.urlParameters.map((urlParameter) => `{${urlParameter}}`).join(', ')} in the server URL.`
        );
    }

    if (authentication.type === 'oauth2' && !authentication.dynamicClientRegistration) {
        notes.push(
            authentication.pkce
                ? 'Enter the Client Id of an OAuth app registered with this server, and its Client Secret if it has one.'
                : 'Enter the Client Id and Client Secret of an OAuth app registered with this server.'
        );
    }

    if (authentication.type === 'header') {
        const otherHeaders = (authentication.headers ?? []).filter((header) => !isAuthorizationHeader(header.name));

        if (otherHeaders.length) {
            notes.push(
                `This server expects the ${otherHeaders.map((header) => header.name).join(', ')} header, which this connection cannot send.`
            );
        }
    }

    return notes;
}

export function getMcpServerCatalogPrefillOption(entry: McpServerCatalogEntryType): ConnectionPrefillOptionI {
    return {
        componentDocumentationUrl: entry.component
            ? `https://docs.bytechef.io/reference/components/${toKebabCase(entry.component)}_v1`
            : undefined,
        componentName: entry.component ?? undefined,
        description: entry.description ?? undefined,
        label: entry.title,
        notes: getMcpServerCatalogNotes(entry),
        prefill: getMcpServerCatalogPrefill(entry),
        value: entry.slug,
        websiteUrl: entry.websiteUrl ?? undefined,
    };
}
