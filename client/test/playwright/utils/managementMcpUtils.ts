import {type APIRequestContext, type Page, expect} from '@playwright/test';

import {postGraphQl} from './aiHubUtils';
import {McpClient, type McpToolCallResultI, toolResultText} from './mcpClient';

const DEVELOPMENT_ENVIRONMENT_ID = '0';

export function readStructuredResult<ResultType>(toolCallResult: McpToolCallResultI): ResultType {
    expect(toolCallResult.isError ?? false, toolResultText(toolCallResult)).toBe(false);

    return (toolCallResult.structuredContent ?? JSON.parse(toolResultText(toolCallResult))) as ResultType;
}

async function readManagementMcpSecretKey(page: Page): Promise<string> {
    const data = await postGraphQl<{managementMcpServerUrl: string}>(page, 'query { managementMcpServerUrl }', {});
    const secretKey = /\/api\/management\/([^/]+)\/mcp/.exec(data.managementMcpServerUrl)?.[1];

    expect(secretKey, 'the management MCP server URL carries its secret key').toBeDefined();

    return secretKey!;
}

async function createAdminApiKey(page: Page, name: string): Promise<string> {
    const data = await postGraphQl<{createApiKey: string}>(
        page,
        'mutation createApiKey($name: String!, $environmentId: ID!) { createApiKey(name: $name, environmentId: $environmentId) }',
        {environmentId: DEVELOPMENT_ENVIRONMENT_ID, name}
    );

    return data.createApiKey;
}

export async function deleteAdminApiKey(page: Page, name: string): Promise<void> {
    const data = await postGraphQl<{adminApiKeys: Array<{id: string; name: string}>}>(
        page,
        'query adminApiKeys($environmentId: ID!) { adminApiKeys(environmentId: $environmentId) { id name } }',
        {environmentId: DEVELOPMENT_ENVIRONMENT_ID}
    );

    for (const apiKey of data.adminApiKeys.filter((candidate) => candidate.name === name)) {
        await postGraphQl(page, 'mutation deleteApiKey($id: ID!) { deleteApiKey(id: $id) }', {id: apiKey.id});
    }
}

/**
 * An initialized client of the management MCP server. Without PLAYWRIGHT_MCP_SECRET_KEY and
 * PLAYWRIGHT_MCP_BEARER_TOKEN it reads the secret key from the app and creates an admin API key named apiKeyName;
 * delete that key with deleteAdminApiKey afterwards.
 */
export async function connectManagementMcp(
    page: Page,
    request: APIRequestContext,
    apiKeyName: string
): Promise<McpClient> {
    const bearerToken = process.env.PLAYWRIGHT_MCP_BEARER_TOKEN || (await createAdminApiKey(page, apiKeyName));
    const mcpClient = new McpClient(
        request,
        process.env.PLAYWRIGHT_MCP_SECRET_KEY || (await readManagementMcpSecretKey(page)),
        bearerToken
    );

    await mcpClient.initialize();

    return mcpClient;
}
