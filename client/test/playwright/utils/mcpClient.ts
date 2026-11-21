import {type APIRequestContext, expect} from '@playwright/test';

export interface McpToolI {
    description?: string;
    inputSchema?: {properties?: Record<string, unknown>; required?: string[]};
    name: string;
}

export interface McpToolCallResultI {
    content: Array<{text?: string; type: string}>;
    isError?: boolean;
    structuredContent?: unknown;
}

interface JsonRpcResponseI<ResultType> {
    error?: {code: number; message: string};
    id?: number;
    result?: ResultType;
}

const MCP_PROTOCOL_VERSION = '2025-06-18';

function parseJsonRpcBody<ResultType>(body: string, contentType: string): JsonRpcResponseI<ResultType> {
    if (!contentType.includes('text/event-stream')) {
        return JSON.parse(body);
    }

    const dataLines = body
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice('data:'.length).trim())
        .filter((data) => data.length > 0);

    const lastDataLine = dataLines.at(-1);

    if (lastDataLine === undefined) {
        throw new Error(`The MCP server answered with an empty event stream: ${body}`);
    }

    return JSON.parse(lastDataLine);
}

export class McpClient {
    private nextRequestId = 1;
    private sessionId: string | undefined;

    private readonly bearerToken: string | undefined;
    private readonly endpointPath: string;
    private readonly environment: string;
    private readonly request: APIRequestContext;

    constructor(request: APIRequestContext, secretKey: string) {
        this.request = request;
        this.endpointPath = `/api/management/${secretKey}/mcp`;
        this.bearerToken = process.env.PLAYWRIGHT_MCP_BEARER_TOKEN;
        this.environment = process.env.PLAYWRIGHT_MCP_ENVIRONMENT ?? 'DEVELOPMENT';
    }

    async initialize(): Promise<void> {
        const response = await this.post('initialize', {
            capabilities: {},
            clientInfo: {name: 'bytechef-playwright', version: '1.0.0'},
            protocolVersion: MCP_PROTOCOL_VERSION,
        });

        expect(response.error).toBeUndefined();

        await this.notify('notifications/initialized');
    }

    async listTools(): Promise<McpToolI[]> {
        const response = await this.post<{tools: McpToolI[]}>('tools/list', {});

        expect(response.error).toBeUndefined();

        return response.result?.tools ?? [];
    }

    async callTool(name: string, toolArguments: Record<string, unknown>): Promise<McpToolCallResultI> {
        const response = await this.post<McpToolCallResultI>('tools/call', {arguments: toolArguments, name});

        expect(response.error).toBeUndefined();

        return response.result as McpToolCallResultI;
    }

    private headers(): Record<string, string> {
        const headers: Record<string, string> = {
            Accept: 'application/json, text/event-stream',
            'Content-Type': 'application/json',
            'MCP-Protocol-Version': MCP_PROTOCOL_VERSION,
            'X-ENVIRONMENT': this.environment,
        };

        if (this.bearerToken) {
            headers.Authorization = `Bearer ${this.bearerToken}`;
        }

        if (this.sessionId !== undefined) {
            headers['Mcp-Session-Id'] = this.sessionId;
        }

        return headers;
    }

    private async notify(method: string): Promise<void> {
        await this.request.post(this.endpointPath, {
            data: {jsonrpc: '2.0', method},
            headers: this.headers(),
        });
    }

    private async post<ResultType>(method: string, params: unknown): Promise<JsonRpcResponseI<ResultType>> {
        const response = await this.request.post(this.endpointPath, {
            data: {id: this.nextRequestId++, jsonrpc: '2.0', method, params},
            headers: this.headers(),
            timeout: 300000,
        });

        expect(response.ok(), `${method} answered ${response.status()}: ${await response.text()}`).toBe(true);

        const responseSessionId = response.headers()['mcp-session-id'];

        if (responseSessionId) {
            this.sessionId = responseSessionId;
        }

        return parseJsonRpcBody<ResultType>(await response.text(), response.headers()['content-type'] ?? '');
    }
}

export function toolResultText(toolCallResult: McpToolCallResultI): string {
    return toolCallResult.content
        .filter((contentItem) => contentItem.type === 'text')
        .map((contentItem) => contentItem.text ?? '')
        .join('\n');
}
