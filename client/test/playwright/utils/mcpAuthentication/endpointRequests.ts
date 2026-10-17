import {type APIRequestContext, type APIResponse} from '@playwright/test';

import {type BackendSession} from './backendSession';
import {type EnvironmentNameType} from './mcpTestConfiguration';

const MCP_INITIALIZE_REQUEST = {
    id: 1,
    jsonrpc: '2.0',
    method: 'initialize',
    params: {
        capabilities: {},
        clientInfo: {name: 'playwright-mcp-authentication', version: '1'},
        protocolVersion: '2025-06-18',
    },
};

const A2A_GET_TASK_REQUEST = {
    id: 1,
    jsonrpc: '2.0',
    method: 'tasks/get',
    params: {id: '00000000-0000-0000-0000-000000000000'},
};

export interface EndpointCredentialI {
    bearerToken?: string;
    environment?: EnvironmentNameType;
    session?: BackendSession;
}

async function postJsonRpc(
    requestContext: APIRequestContext,
    url: string,
    body: unknown,
    {bearerToken, environment, session}: EndpointCredentialI
): Promise<APIResponse> {
    const headers: Record<string, string> = {
        Accept: 'application/json, text/event-stream',
        'Content-Type': 'application/json',
    };

    if (bearerToken) {
        headers.Authorization = `Bearer ${bearerToken}`;
    }

    if (environment) {
        headers['X-ENVIRONMENT'] = environment;
    }

    if (session) {
        Object.assign(headers, await session.getXsrfHeaders());
    }

    const targetRequestContext = session ? session.requestContext : requestContext;

    return targetRequestContext.post(url, {data: body, headers, maxRedirects: 0});
}

export async function postMcpInitialize(
    requestContext: APIRequestContext,
    url: string,
    credential: EndpointCredentialI = {}
): Promise<number> {
    const response = await postJsonRpc(requestContext, url, MCP_INITIALIZE_REQUEST, credential);

    return response.status();
}

export async function postMcpInitializeResponse(
    requestContext: APIRequestContext,
    url: string,
    credential: EndpointCredentialI = {}
): Promise<APIResponse> {
    return postJsonRpc(requestContext, url, MCP_INITIALIZE_REQUEST, credential);
}

export async function postA2aTaskRequest(
    requestContext: APIRequestContext,
    url: string,
    credential: EndpointCredentialI = {}
): Promise<number> {
    const response = await postJsonRpc(requestContext, url, A2A_GET_TASK_REQUEST, credential);

    return response.status();
}
