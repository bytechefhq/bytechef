import {
    type McpAuthenticationEnvironmentI,
    type OAuth2TokenProviderI,
    mcpAuthenticationTest as test,
} from '../../fixtures/mcpAuthentication';
import {type EndpointCredentialI, postA2aTaskRequest, postMcpInitialize} from './endpointRequests';
import {MEMBER_CREDENTIALS_MISSING_REASON, hasWorkspaceMembers} from './mcpTestConfiguration';

interface StatusCaseContextI {
    mcpEnvironment: McpAuthenticationEnvironmentI;
    oauth2Tokens: OAuth2TokenProviderI;
}

export interface StatusCaseI {
    credential?: (context: StatusCaseContextI) => EndpointCredentialI | Promise<EndpointCredentialI>;
    description: string;
    expectedStatus: number;
    id: string;
    protocol?: 'a2a' | 'mcp';
    requiresWorkspaceMembers?: boolean;
    url: (mcpEnvironment: McpAuthenticationEnvironmentI) => string;
}

export function defineStatusCases(statusCases: StatusCaseI[]): void {
    for (const statusCase of statusCases) {
        test(`${statusCase.id} ${statusCase.description} → ${statusCase.expectedStatus}`, async ({
            mcpEnvironment,
            oauth2Tokens,
            request,
        }) => {
            test.skip(
                Boolean(statusCase.requiresWorkspaceMembers) && !hasWorkspaceMembers(),
                MEMBER_CREDENTIALS_MISSING_REASON
            );

            const credential = statusCase.credential ? await statusCase.credential({mcpEnvironment, oauth2Tokens}) : {};

            const url = statusCase.url(mcpEnvironment);

            const status =
                statusCase.protocol === 'a2a'
                    ? await postA2aTaskRequest(request, url, credential)
                    : await postMcpInitialize(request, url, credential);

            test.expect(status).toBe(statusCase.expectedStatus);
        });
    }
}
