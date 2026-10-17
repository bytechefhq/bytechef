import {
    AUTOMATION_SCOPE,
    MANAGEMENT_SCOPE,
    type McpAuthenticationEnvironmentI,
    mcpAuthenticationTest as test,
} from '../../fixtures/mcpAuthentication';
import {postMcpInitialize, postMcpInitializeResponse} from '../../utils/mcpAuthentication/endpointRequests';
import {type EnvironmentNameType, MCP_TEST_CONFIGURATION} from '../../utils/mcpAuthentication/mcpTestConfiguration';
import {
    type OAuth2TokensI,
    createAuthorizationUrl,
    createPkcePair,
    registerPublicClient,
    requestTokens,
} from '../../utils/mcpAuthentication/oauth2Client';

const {expect} = test;

const AUDIT_FIXME_REASON =
    'No API exposes the persisted audit events; the live run checked persistent_audit_event with SQL';

const PLATFORM_DEPLOY_PATH = '/api/platform/v1/custom-components/deploy';

interface ProtectedResourceMetadataI {
    authorization_servers: string[];
    resource: string;
    scopes_supported: string[];
    tls_client_certificate_bound_access_tokens: boolean;
}

test.describe('OAuth2 sign-in for MCP servers (#3213)', () => {
    test.describe('discovery', () => {
        const discoveryTargets = [
            {
                label: 'management',
                scope: MANAGEMENT_SCOPE,
                url: (mcpEnvironment: McpAuthenticationEnvironmentI) => mcpEnvironment.managementMcpUrls.tenantLess,
            },
            {
                label: 'automation',
                scope: AUTOMATION_SCOPE,
                url: (mcpEnvironment: McpAuthenticationEnvironmentI) =>
                    mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
            },
        ];

        for (const discoveryTarget of discoveryTargets) {
            test(`discovery ${discoveryTarget.label}: 401 challenge names public metadata URL and scope`, async ({
                mcpEnvironment,
                request,
            }) => {
                const response = await postMcpInitializeResponse(request, discoveryTarget.url(mcpEnvironment));

                expect(response.status()).toBe(401);

                const challenge = response.headers()['www-authenticate'] ?? '';

                expect(challenge).toContain(
                    `resource_metadata="${MCP_TEST_CONFIGURATION.publicUrl}/.well-known/oauth-protected-resource/api/`
                );
                expect(challenge).toContain(`scope="${discoveryTarget.scope}"`);
            });

            test(`discovery ${discoveryTarget.label}: metadata resource, scopes, issuer`, async ({
                mcpEnvironment,
                request,
            }) => {
                const resourceUrl = discoveryTarget.url(mcpEnvironment);

                const response = await request.get(
                    `${MCP_TEST_CONFIGURATION.publicUrl}/.well-known/oauth-protected-resource${new URL(resourceUrl).pathname}`
                );

                expect(response.status()).toBe(200);

                const metadata = (await response.json()) as ProtectedResourceMetadataI;

                expect(metadata.resource).toBe(resourceUrl);
                expect(metadata.scopes_supported).toEqual([discoveryTarget.scope]);
                expect(metadata.authorization_servers).toEqual([MCP_TEST_CONFIGURATION.publicUrl]);
                expect(metadata.tls_client_certificate_bound_access_tokens).toBe(false);
            });
        }
    });

    test.describe('refresh and revocation', () => {
        test.describe.configure({mode: 'serial'});

        let initialTokens: OAuth2TokensI;
        let refreshedTokens: {access_token: string; expires_in: number; refresh_token: string} | undefined;

        test.beforeAll(async ({mcpEnvironment, oauth2Tokens}) => {
            initialTokens = await oauth2Tokens.issueTokens(
                'admin',
                mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                AUTOMATION_SCOPE
            );
        });

        test('refresh: public client exchanges its refresh token', async ({request}) => {
            expect(initialTokens.refreshToken).toBeTruthy();

            const response = await requestTokens(request, {
                client_id: initialTokens.clientId,
                grant_type: 'refresh_token',
                refresh_token: initialTokens.refreshToken!,
            });

            expect(response.status()).toBe(200);

            refreshedTokens = await response.json();

            expect(refreshedTokens?.access_token).toBeTruthy();
            expect(refreshedTokens?.refresh_token).toBeTruthy();
        });

        test('refresh: access token lifetime is one hour', () => {
            expect(refreshedTokens?.expires_in).toBeGreaterThanOrEqual(3500);
            expect(refreshedTokens?.expires_in).toBeLessThanOrEqual(3600);
        });

        test('refresh: refresh token is rotated', () => {
            expect(refreshedTokens?.refresh_token).toBeTruthy();
            expect(refreshedTokens?.refresh_token).not.toBe(initialTokens.refreshToken);
        });

        test('refresh: refreshed access token works on its endpoint', async ({mcpEnvironment, request}) => {
            const status = await postMcpInitialize(
                request,
                mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                {bearerToken: refreshedTokens!.access_token}
            );

            expect(status).toBe(200);
        });

        test('refresh: reusing the old refresh token is rejected', async ({request}) => {
            const response = await requestTokens(request, {
                client_id: initialTokens.clientId,
                grant_type: 'refresh_token',
                refresh_token: initialTokens.refreshToken!,
            });

            expect(response.status()).toBe(400);
            expect(await response.text()).toContain('invalid_grant');
        });

        test('refresh: unknown client_id is rejected', async ({request}) => {
            const response = await requestTokens(request, {
                client_id: 'not-a-client',
                grant_type: 'refresh_token',
                refresh_token: refreshedTokens!.refresh_token,
            });

            expect(response.status()).toBe(401);
            expect(await response.text()).toContain('invalid_client');
        });

        test('revocation: grant revoked from the OAuth2 Clients API', async ({mcpEnvironment}) => {
            const {adminSession} = mcpEnvironment;

            const data = await adminSession.graphQlData<{registeredClients: {clientId: string; id: string}[]}>(
                'query registeredClients { registeredClients { id clientId } }'
            );

            const registeredClient = data.registeredClients.find(
                (candidate) => candidate.clientId === initialTokens.clientId
            );

            expect(registeredClient).toBeDefined();

            const revocation = await adminSession.graphQlData<{revokeRegisteredClientGrant: boolean}>(
                `mutation revokeRegisteredClientGrant($id: ID!, $principalName: String!) {
                    revokeRegisteredClientGrant(registeredClientId: $id, principalName: $principalName)
                }`,
                {id: registeredClient!.id, principalName: adminSession.credentials.email}
            );

            expect(revocation.revokeRegisteredClientGrant).toBe(true);
        });

        test('revocation: access token is rejected afterwards', async ({mcpEnvironment, request}) => {
            const status = await postMcpInitialize(
                request,
                mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                {bearerToken: refreshedTokens!.access_token}
            );

            expect(status).toBe(401);
        });

        test('revocation: refresh token is rejected afterwards', async ({request}) => {
            const response = await requestTokens(request, {
                client_id: initialTokens.clientId,
                grant_type: 'refresh_token',
                refresh_token: refreshedTokens!.refresh_token,
            });

            expect(response.status()).toBe(400);
            expect(await response.text()).toContain('invalid_grant');
        });
    });

    test('consent: foreign redirect gets the consent page, no code', async ({oauth2Tokens, request}) => {
        const foreignRedirectUri = 'https://attacker.example/callback';

        const page = await oauth2Tokens.getPage('admin');
        const clientId = await registerPublicClient(request, foreignRedirectUri);
        const {codeChallenge} = createPkcePair();

        const authorizationUrl = await createAuthorizationUrl(request, {
            clientId,
            codeChallenge,
            redirectUri: foreignRedirectUri,
        });

        const foreignRequests: string[] = [];

        const recordForeignRequest = (pageRequest: {url: () => string}) => {
            if (pageRequest.url().startsWith('https://attacker.example')) {
                foreignRequests.push(pageRequest.url());
            }
        };

        page.on('request', recordForeignRequest);

        try {
            await page.goto(authorizationUrl);

            await expect(page).toHaveURL(/\/oauth2\/consent/);

            await expect(page.getByRole('button', {name: 'Allow'})).toBeVisible();

            expect(page.url()).not.toContain('code=');
            expect(foreignRequests).toEqual([]);

            const deniedCallbackPromise = page.waitForRequest((pageRequest) =>
                pageRequest.url().startsWith(foreignRedirectUri)
            );

            await page.getByRole('button', {name: 'Deny'}).click();

            const deniedCallback = await deniedCallbackPromise;

            expect(new URL(deniedCallback.url()).searchParams.get('code')).toBeNull();
        } finally {
            page.off('request', recordForeignRequest);
        }
    });

    test.describe('platform v1', () => {
        const postPlatformDeploy = async (
            request: Parameters<typeof postMcpInitialize>[0],
            bearerToken: string | undefined,
            environment: EnvironmentNameType
        ): Promise<number> => {
            const headers: Record<string, string> = {'X-ENVIRONMENT': environment};

            if (bearerToken) {
                headers.Authorization = `Bearer ${bearerToken}`;
            }

            const response = await request.post(`${MCP_TEST_CONFIGURATION.publicUrl}${PLATFORM_DEPLOY_PATH}`, {
                data: {},
                headers,
            });

            return response.status();
        };

        test('platform v1: admin key is authenticated (not 401/403)', async ({mcpEnvironment, request}) => {
            const status = await postPlatformDeploy(request, mcpEnvironment.apiKeys.admin, 'DEVELOPMENT');

            expect([401, 403]).not.toContain(status);
        });

        test('platform v1: DEVELOPMENT admin key works with X-ENVIRONMENT=PRODUCTION', async ({
            mcpEnvironment,
            request,
        }) => {
            const status = await postPlatformDeploy(request, mcpEnvironment.apiKeys.admin, 'PRODUCTION');

            expect([401, 403]).not.toContain(status);
        });

        test('platform v1: workspace key is rejected', async ({mcpEnvironment, request}) => {
            expect(await postPlatformDeploy(request, mcpEnvironment.apiKeys.firstWorkspace, 'DEVELOPMENT')).toBe(401);
        });

        test('platform v1: embedded key is rejected', async ({mcpEnvironment, request}) => {
            expect(await postPlatformDeploy(request, mcpEnvironment.apiKeys.embedded, 'DEVELOPMENT')).toBe(401);
        });

        test('platform v1: no key is rejected', async ({request}) => {
            expect(await postPlatformDeploy(request, undefined, 'DEVELOPMENT')).toBe(401);
        });

        test.fixme('platform v1: one rejected key writes exactly one failure audit row', () => {
            test.info().annotations.push({description: AUDIT_FIXME_REASON, type: 'fixme'});
        });
    });

    test.describe('audit', () => {
        test.fixme('audit: no stored principal is a token', () => {
            test.info().annotations.push({description: AUDIT_FIXME_REASON, type: 'fixme'});
        });

        test.fixme('audit: rejected bearer tokens are recorded as [credential]', () => {
            test.info().annotations.push({description: AUDIT_FIXME_REASON, type: 'fixme'});
        });
    });
});
