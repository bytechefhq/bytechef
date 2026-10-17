import {AUTOMATION_SCOPE, MANAGEMENT_SCOPE, mcpAuthenticationTest as test} from '../../fixtures/mcpAuthentication';
import {defineStatusCases} from '../../utils/mcpAuthentication/statusCases';

const FORGED_JWT = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ4In0.c2ln';

test.describe('Management MCP server authentication (#2197)', () => {
    defineStatusCases([
        {
            description: 'no credential (tenant-less URL)',
            expectedStatus: 401,
            id: 'M1',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin, environment: 'DEVELOPMENT'}),
            description: 'admin key, X-ENVIRONMENT=DEVELOPMENT, tenant-less URL',
            expectedStatus: 200,
            id: 'M2',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin, environment: 'DEVELOPMENT'}),
            description: 'admin key, X-ENVIRONMENT=DEVELOPMENT, secret URL',
            expectedStatus: 200,
            id: 'M3',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.secret,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin}),
            description: 'DEVELOPMENT admin key without X-ENVIRONMENT (admin keys work in every environment)',
            expectedStatus: 200,
            id: 'M4',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin, environment: 'PRODUCTION'}),
            description: 'DEVELOPMENT admin key, X-ENVIRONMENT=PRODUCTION',
            expectedStatus: 200,
            id: 'M5',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin, environment: 'STAGING'}),
            description: 'DEVELOPMENT admin key, X-ENVIRONMENT=STAGING, secret URL',
            expectedStatus: 200,
            id: 'M5b',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.secret,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.firstWorkspace,
                environment: 'DEVELOPMENT',
            }),
            description: 'workspace key',
            expectedStatus: 401,
            id: 'M6',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.embedded,
                environment: 'DEVELOPMENT',
            }),
            description: 'embedded-type key',
            expectedStatus: 401,
            id: 'M7',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: ({mcpEnvironment}) => ({session: mcpEnvironment.adminSession}),
            description: 'UI session cookie',
            expectedStatus: 401,
            id: 'M8',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin, environment: 'DEVELOPMENT'}),
            description: 'admin key, wrong secret in URL',
            expectedStatus: 401,
            id: 'M9',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.secret.replace(/\/mcp$/, 'x/mcp'),
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.managementMcpUrls.tenantLess,
                    MANAGEMENT_SCOPE
                ),
            }),
            description: 'OAuth admin token (aud tenant-less)',
            expectedStatus: 200,
            id: 'M10',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.managementMcpUrls.secret,
                    MANAGEMENT_SCOPE
                ),
            }),
            description: 'OAuth admin token (aud secret URL)',
            expectedStatus: 200,
            id: 'M11',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.secret,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.managementMcpUrls.tenantLess,
                    MANAGEMENT_SCOPE
                ),
            }),
            description: 'OAuth token issued for the tenant-less URL used on the secret URL',
            expectedStatus: 401,
            id: 'M12',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.secret,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'admin',
                    mcpEnvironment.automationMcpServers.requiredFirstWorkspace.url,
                    AUTOMATION_SCOPE
                ),
            }),
            description: 'OAuth mcp:automation token on management',
            expectedStatus: 403,
            id: 'M13',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: () => ({bearerToken: FORGED_JWT}),
            description: 'forged JWT',
            expectedStatus: 401,
            id: 'M14',
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
        {
            credential: async ({mcpEnvironment, oauth2Tokens}) => ({
                bearerToken: await oauth2Tokens.getAccessToken(
                    'editor',
                    mcpEnvironment.managementMcpUrls.tenantLess,
                    MANAGEMENT_SCOPE
                ),
            }),
            description: 'OAuth non-admin member token',
            expectedStatus: 200,
            id: 'M15',
            requiresWorkspaceMembers: true,
            url: (mcpEnvironment) => mcpEnvironment.managementMcpUrls.tenantLess,
        },
    ]);
});
