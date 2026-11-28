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

const LINEAR_ENTRY = createEntry({
    authentication: createAuthentication({
        authorizationUrl: 'https://linear.app/oauth/authorize',
        dynamicClientRegistration: false,
        scopes: ['read'],
        tokenUrl: 'https://api.linear.app/oauth/token',
        type: 'oauth2',
    }),
    component: 'linear',
    slug: 'linear',
    title: 'Linear',
    url: 'https://mcp.linear.app/mcp',
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
            credentialsRequired: false,
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
            credentialsRequired: true,
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
            credentialsRequired: true,
            parameters: {
                authorizationUrl: 'https://elevenlabs.io/app/oauth/authorize',
                tokenUrl: 'https://api.us.elevenlabs.io/v1/oauth/token',
                transportType: 'httpStreamable',
                url: 'https://api.elevenlabs.io/mcp',
            },
        });
    });

    it('signs in with the predefined app of the entry component when the server has no dynamic client registration', () => {
        const prefill = getMcpServerCatalogPrefill(LINEAR_ENTRY, ['github', 'linear']);

        expect(prefill).toEqual({
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
            credentialsRequired: false,
            parameters: {
                authorizationUrl: 'https://linear.app/oauth/authorize',
                scopes: 'read',
                tokenUrl: 'https://api.linear.app/oauth/token',
                transportType: 'httpStreamable',
                url: 'https://mcp.linear.app/mcp',
            },
        });
    });

    it('asks for credentials when the entry component has no predefined app', () => {
        expect(getMcpServerCatalogPrefill(LINEAR_ENTRY, ['github'])).toEqual(
            expect.objectContaining({authorizationType: 'OAUTH2_AUTHORIZATION_CODE', credentialsRequired: true})
        );
    });

    it('asks for credentials when an entry with a predefined app lacks its endpoints', () => {
        const entry = createEntry({
            authentication: createAuthentication({
                authorizationUrl: 'https://linear.app/oauth/authorize',
                dynamicClientRegistration: false,
                type: 'oauth2',
            }),
            component: 'linear',
        });

        expect(getMcpServerCatalogPrefill(entry, ['linear']).credentialsRequired).toBe(true);
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
    it('carries the entry details', () => {
        const option = getMcpServerCatalogPrefillOption(
            createEntry({
                description: 'Official server',
                websiteUrl: 'https://example.com/docs',
            })
        );

        expect(option).toEqual(
            expect.objectContaining({
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

    it('does not ask for credentials of an entry whose component has a predefined app', () => {
        expect(getMcpServerCatalogPrefillOption(LINEAR_ENTRY, ['linear']).notes).toEqual([]);
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
