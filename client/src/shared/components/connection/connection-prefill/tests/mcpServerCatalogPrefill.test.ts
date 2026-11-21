import {describe, expect, it} from 'vitest';

import {
    McpServerCatalogEntryType,
    getMcpServerCatalogPrefill,
    getMcpServerCatalogPrefillOption,
} from '../mcp-client/mcpServerCatalogPrefill';

type AuthenticationType = McpServerCatalogEntryType['authentication'];

type HeaderType = NonNullable<AuthenticationType['headers']>[number];

const createAuthentication = (overrides: Partial<AuthenticationType> & {type: string}): AuthenticationType => ({
    authorizationUrl: null,
    clientIdMetadataDocument: null,
    dynamicClientRegistration: null,
    headers: null,
    pkce: null,
    scopes: null,
    tokenUrl: null,
    ...overrides,
});

const createHeader = (name: string): HeaderType => ({description: null, name, secret: true});

const createEntry = (overrides: Partial<McpServerCatalogEntryType> = {}): McpServerCatalogEntryType => ({
    authentication: createAuthentication({type: 'none'}),
    component: null,
    description: null,
    slug: 'example',
    title: 'Example',
    transportType: 'httpStreamable',
    url: 'https://mcp.example.com/mcp',
    urlParameters: null,
    websiteUrl: null,
    ...overrides,
});

describe('getMcpServerCatalogPrefill', () => {
    it('maps an oauth2 entry with dynamic client registration to the automatic OAuth2 authorization', () => {
        const prefill = getMcpServerCatalogPrefill(
            createEntry({
                authentication: createAuthentication({
                    dynamicClientRegistration: true,
                    scopes: ['read'],
                    type: 'oauth2',
                }),
                url: 'https://mcp.notion.com/mcp',
            })
        );

        expect(prefill).toEqual({
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
            parameters: {transportType: 'httpStreamable', url: 'https://mcp.notion.com/mcp'},
        });
    });

    it('maps an oauth2 entry without dynamic client registration to the manual OAuth2 authorization', () => {
        const prefill = getMcpServerCatalogPrefill(
            createEntry({
                authentication: createAuthentication({
                    authorizationUrl: 'https://github.com/login/oauth/authorize',
                    dynamicClientRegistration: false,
                    scopes: ['repo', 'read:org'],
                    tokenUrl: 'https://github.com/login/oauth/access_token',
                    type: 'oauth2',
                }),
            })
        );

        expect(prefill).toEqual({
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
            parameters: {
                authorizationUrl: 'https://github.com/login/oauth/authorize',
                scopes: 'repo,read:org',
                tokenUrl: 'https://github.com/login/oauth/access_token',
                transportType: 'httpStreamable',
                url: 'https://mcp.example.com/mcp',
            },
        });
    });

    it('maps a manual oauth2 entry that requires PKCE to the PKCE authorization with its endpoints', () => {
        const prefill = getMcpServerCatalogPrefill(
            createEntry({
                authentication: createAuthentication({
                    authorizationUrl: 'https://elevenlabs.io/app/oauth/authorize',
                    dynamicClientRegistration: false,
                    pkce: true,
                    tokenUrl: 'https://api.us.elevenlabs.io/v1/oauth/token',
                    type: 'oauth2',
                }),
                slug: 'elevenlabs',
                title: 'ElevenLabs',
                url: 'https://api.elevenlabs.io/mcp',
            })
        );

        expect(prefill).toEqual({
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
            parameters: {
                authorizationUrl: 'https://elevenlabs.io/app/oauth/authorize',
                tokenUrl: 'https://api.us.elevenlabs.io/v1/oauth/token',
                transportType: 'httpStreamable',
                url: 'https://api.elevenlabs.io/mcp',
            },
        });
    });

    it('maps a header entry that only needs the Authorization header to the bearer token authorization', () => {
        const prefill = getMcpServerCatalogPrefill(
            createEntry({
                authentication: createAuthentication({headers: [createHeader('Authorization')], type: 'header'}),
                transportType: 'sse',
            })
        );

        expect(prefill).toEqual({
            authorizationType: 'BEARER_TOKEN',
            parameters: {transportType: 'sse', url: 'https://mcp.example.com/mcp'},
        });
    });

    it('leaves the authorization unset for a header entry with other headers', () => {
        const prefill = getMcpServerCatalogPrefill(
            createEntry({authentication: createAuthentication({headers: [createHeader('X-Api-Key')], type: 'header'})})
        );

        expect(prefill.authorizationType).toBeUndefined();
    });

    it('maps a none entry to no authorization', () => {
        expect(getMcpServerCatalogPrefill(createEntry())).toEqual({
            authorizationType: undefined,
            parameters: {transportType: 'httpStreamable', url: 'https://mcp.example.com/mcp'},
        });
    });

    it('skips an unknown transport type', () => {
        expect(getMcpServerCatalogPrefill(createEntry({transportType: 'websocket'})).parameters).toEqual({
            url: 'https://mcp.example.com/mcp',
        });
    });
});

describe('getMcpServerCatalogPrefillOption', () => {
    it('carries the entry details and links the matching ByteChef component', () => {
        const option = getMcpServerCatalogPrefillOption(
            createEntry({
                component: 'activeCampaign',
                description: 'Official server',
                websiteUrl: 'https://example.com/docs',
            })
        );

        expect(option).toEqual(
            expect.objectContaining({
                componentDocumentationUrl: 'https://docs.bytechef.io/reference/components/active-campaign_v1',
                componentName: 'activeCampaign',
                description: 'Official server',
                label: 'Example',
                notes: [],
                value: 'example',
                websiteUrl: 'https://example.com/docs',
            })
        );
    });

    it('asks only for the client id, and an optional client secret, of a manual PKCE entry', () => {
        expect(
            getMcpServerCatalogPrefillOption(
                createEntry({
                    authentication: createAuthentication({
                        dynamicClientRegistration: false,
                        pkce: true,
                        type: 'oauth2',
                    }),
                })
            ).notes
        ).toEqual([
            'Enter the Client Id of an OAuth app registered with this server, and its Client Secret if it has one.',
        ]);
    });

    it('explains URL placeholders, manual OAuth2 credentials and unsupported headers', () => {
        expect(
            getMcpServerCatalogPrefillOption(
                createEntry({
                    authentication: createAuthentication({dynamicClientRegistration: false, type: 'oauth2'}),
                    url: 'https://{host}/api/2.0/mcp/genie',
                    urlParameters: ['host'],
                })
            ).notes
        ).toEqual([
            'Replace {host} in the server URL.',
            'Enter the Client Id and Client Secret of an OAuth app registered with this server.',
        ]);

        expect(
            getMcpServerCatalogPrefillOption(
                createEntry({
                    authentication: createAuthentication({headers: [createHeader('X-Api-Key')], type: 'header'}),
                })
            ).notes
        ).toEqual(['This server expects the X-Api-Key header, which this connection cannot send.']);
    });
});
