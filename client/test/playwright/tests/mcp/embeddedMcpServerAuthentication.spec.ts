import {mcpAuthenticationTest as test} from '../../fixtures/mcpAuthentication';
import {defineStatusCases} from '../../utils/mcpAuthentication/statusCases';

test.describe('Embedded MCP server authentication (#2446)', () => {
    defineStatusCases([
        {
            credential: () => ({environment: 'DEVELOPMENT'}),
            description: 'required: no credential',
            expectedStatus: 401,
            id: 'E1',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.developmentSigned,
                environment: 'DEVELOPMENT',
            }),
            description: 'required: DEVELOPMENT-signed JWT, X-ENVIRONMENT matches',
            expectedStatus: 200,
            id: 'E2',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.developmentSigned,
                environment: 'PRODUCTION',
            }),
            description: 'required: DEVELOPMENT-signed JWT, X-ENVIRONMENT=PRODUCTION',
            expectedStatus: 401,
            id: 'E3',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.connectedUserJwts.developmentSigned}),
            description: 'required: DEVELOPMENT-signed JWT, no X-ENVIRONMENT (defaults to PRODUCTION)',
            expectedStatus: 401,
            id: 'E4',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.stagingSigned,
                environment: 'STAGING',
            }),
            description: 'required: STAGING-signed JWT, X-ENVIRONMENT=STAGING (server is DEVELOPMENT)',
            expectedStatus: 401,
            id: 'E5',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.stagingSigned,
                environment: 'DEVELOPMENT',
            }),
            description: 'required: STAGING-signed JWT, X-ENVIRONMENT=DEVELOPMENT (wrong signing key)',
            expectedStatus: 401,
            id: 'E6',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.unrelatedKeySigned,
                environment: 'DEVELOPMENT',
            }),
            description: 'required: JWT with the DEVELOPMENT kid but signed by another key',
            expectedStatus: 401,
            id: 'E7',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.unsigned,
                environment: 'DEVELOPMENT',
            }),
            description: 'required: unsigned JWT (alg none)',
            expectedStatus: 401,
            id: 'E8',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.disabledConnectedUser,
                environment: 'DEVELOPMENT',
            }),
            description: 'required: valid JWT of a disabled connected user',
            expectedStatus: 401,
            id: 'E9',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.firstWorkspace,
                environment: 'DEVELOPMENT',
            }),
            description: 'required: workspace API key',
            expectedStatus: 401,
            id: 'E10',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({bearerToken: mcpEnvironment.apiKeys.admin, environment: 'DEVELOPMENT'}),
            description: 'required: admin API key',
            expectedStatus: 401,
            id: 'E11',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.apiKeys.embedded,
                environment: 'DEVELOPMENT',
            }),
            description: 'required: embedded-type API key',
            expectedStatus: 401,
            id: 'E12',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            credential: ({mcpEnvironment}) => ({environment: 'DEVELOPMENT', session: mcpEnvironment.adminSession}),
            description: 'required: UI session cookie',
            expectedStatus: 401,
            id: 'E13',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.required.url,
        },
        {
            description: 'optional: no credential, no X-ENVIRONMENT',
            expectedStatus: 200,
            id: 'E14',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.optional.url,
        },
        {
            credential: () => ({environment: 'DEVELOPMENT'}),
            description: 'optional: no credential, X-ENVIRONMENT matches',
            expectedStatus: 200,
            id: 'E15',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.optional.url,
        },
        {
            credential: () => ({environment: 'PRODUCTION'}),
            description: 'optional: no credential, X-ENVIRONMENT=PRODUCTION',
            expectedStatus: 401,
            id: 'E16',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.optional.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.developmentSigned,
                environment: 'DEVELOPMENT',
            }),
            description: 'optional: valid JWT (served anonymously)',
            expectedStatus: 200,
            id: 'E17',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.optional.url,
        },
        {
            credential: ({mcpEnvironment}) => ({
                bearerToken: mcpEnvironment.connectedUserJwts.developmentSigned,
                environment: 'DEVELOPMENT',
            }),
            description: 'disabled server: valid JWT',
            expectedStatus: 403,
            id: 'E18',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.disabled.url,
        },
        {
            credential: () => ({environment: 'DEVELOPMENT'}),
            description: 'disabled server: no credential',
            expectedStatus: 401,
            id: 'E19',
            url: (mcpEnvironment) => mcpEnvironment.embeddedMcpServers.disabled.url,
        },
    ]);
});
