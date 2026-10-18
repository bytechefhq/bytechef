import {randomUUID} from 'node:crypto';

const MCP_PROTOCOL_VERSION = '2025-06-18';

export interface JsonRpcMessageI {
    error?: {code: number; message: string};
    id?: number | string;
    jsonrpc: '2.0';
    method?: string;
    params?: Record<string, unknown>;
    result?: Record<string, unknown>;
}

export type ElicitationHandlerType = (elicitationRequest: JsonRpcMessageI) => Promise<Record<string, unknown>>;

function parseJsonRpcMessages(responseText: string): JsonRpcMessageI[] {
    const trimmedText = responseText.trim();

    if (trimmedText.startsWith('{')) {
        return [JSON.parse(trimmedText) as JsonRpcMessageI];
    }

    return trimmedText
        .split(/\r?\n/)
        .filter((responseLine) => responseLine.startsWith('data:'))
        .map((responseLine) => responseLine.slice('data:'.length).trim())
        .filter((jsonText) => jsonText.startsWith('{'))
        .map((jsonText) => JSON.parse(jsonText) as JsonRpcMessageI);
}

/**
 * A minimal Streamable-HTTP MCP client: plain JSON-RPC over POST, reading SSE responses, and answering the
 * server's `elicitation/create` requests that arrive on a pending `tools/call` stream.
 */
export class McpTestClient {
    private nextRequestId = 1;
    private sessionId: string | undefined;

    constructor(
        private readonly endpointUrl: string,
        private readonly capabilities: Record<string, unknown> = {}
    ) {}

    private async post(body: JsonRpcMessageI): Promise<Response> {
        return fetch(this.endpointUrl, {
            body: JSON.stringify(body),
            headers: {
                Accept: 'application/json, text/event-stream',
                'Content-Type': 'application/json',
                'MCP-Protocol-Version': MCP_PROTOCOL_VERSION,
                ...(this.sessionId ? {'Mcp-Session-Id': this.sessionId} : {}),
            },
            method: 'POST',
        });
    }

    private async request(method: string, params: Record<string, unknown> = {}): Promise<JsonRpcMessageI> {
        const requestId = this.nextRequestId++;

        const response = await this.post({id: requestId, jsonrpc: '2.0', method, params});

        const responseMessages = parseJsonRpcMessages(await response.text());

        const responseMessage = responseMessages.find((message) => message.id === requestId);

        if (!responseMessage) {
            throw new Error(`No JSON-RPC response to ${method} (HTTP ${response.status})`);
        }

        return responseMessage;
    }

    async initialize(): Promise<void> {
        const requestId = this.nextRequestId++;

        const response = await this.post({
            id: requestId,
            jsonrpc: '2.0',
            method: 'initialize',
            params: {
                capabilities: this.capabilities,
                clientInfo: {name: 'playwright-mcp-client', version: '1'},
                protocolVersion: MCP_PROTOCOL_VERSION,
            },
        });

        this.sessionId = response.headers.get('Mcp-Session-Id') ?? undefined;

        await response.text();

        const initializedResponse = await this.post({jsonrpc: '2.0', method: 'notifications/initialized'});

        await initializedResponse.text();
    }

    async listToolNames(): Promise<string[]> {
        const listResponse = await this.request('tools/list');

        const tools = (listResponse.result?.tools ?? []) as Array<{name: string}>;

        return tools.map((tool) => tool.name);
    }

    /**
     * Calls a tool and reads its response stream until the result arrives, answering any elicitation request on
     * the way with `elicitationHandler`.
     */
    async callTool(
        toolName: string,
        toolArguments: Record<string, unknown>,
        elicitationHandler?: ElicitationHandlerType
    ): Promise<{elicitationRequests: JsonRpcMessageI[]; result: JsonRpcMessageI}> {
        const requestId = this.nextRequestId++;

        const response = await this.post({
            id: requestId,
            jsonrpc: '2.0',
            method: 'tools/call',
            params: {arguments: toolArguments, name: toolName},
        });

        const elicitationRequests: JsonRpcMessageI[] = [];

        if (!response.body) {
            throw new Error('The tools/call response has no body');
        }

        const streamReader = response.body.getReader();
        const textDecoder = new TextDecoder();

        let bufferedText = '';

        while (true) {
            const {done, value} = await streamReader.read();

            if (value) {
                bufferedText += textDecoder.decode(value, {stream: true});
            }

            const completeLines = bufferedText.split(/\r?\n/);

            bufferedText = done ? '' : (completeLines.pop() ?? '');

            const linesToParse = done ? [...completeLines, bufferedText] : completeLines;

            for (const message of parseJsonRpcMessages(linesToParse.join('\n'))) {
                if (message.method === 'elicitation/create') {
                    elicitationRequests.push(message);

                    if (elicitationHandler) {
                        const elicitationResult = await elicitationHandler(message);

                        const elicitationResponse = await this.post({
                            id: message.id,
                            jsonrpc: '2.0',
                            result: elicitationResult,
                        });

                        await elicitationResponse.text();
                    }
                } else if (message.id === requestId) {
                    await streamReader.cancel();

                    return {elicitationRequests, result: message};
                }
            }

            if (done) {
                throw new Error(`The tools/call stream closed without a result for ${toolName}`);
            }
        }
    }
}

export function getToolResultText(toolResult: JsonRpcMessageI): string {
    const content = (toolResult.result?.content ?? []) as Array<{text?: string}>;

    return content.map((contentItem) => contentItem.text ?? '').join('');
}

export async function sendA2aMessage(endpointUrl: string, text: string): Promise<JsonRpcMessageI> {
    const response = await fetch(endpointUrl, {
        body: JSON.stringify({
            id: 1,
            jsonrpc: '2.0',
            method: 'message/send',
            params: {message: {kind: 'message', messageId: randomUUID(), parts: [{kind: 'text', text}], role: 'user'}},
        }),
        headers: {Accept: 'application/json', 'Content-Type': 'application/json'},
        method: 'POST',
    });

    return parseJsonRpcMessages(await response.text())[0];
}

export async function getA2aTask(endpointUrl: string, taskId: string): Promise<JsonRpcMessageI> {
    const response = await fetch(endpointUrl, {
        body: JSON.stringify({id: 2, jsonrpc: '2.0', method: 'tasks/get', params: {id: taskId}}),
        headers: {Accept: 'application/json', 'Content-Type': 'application/json'},
        method: 'POST',
    });

    return parseJsonRpcMessages(await response.text())[0];
}

export interface A2aTaskStatusI {
    message?: {parts?: Array<{text?: string}>};
    state: string;
}

export function getA2aTaskStatus(taskResponse: JsonRpcMessageI): A2aTaskStatusI {
    return taskResponse.result?.status as A2aTaskStatusI;
}

export function getA2aStatusText(taskResponse: JsonRpcMessageI): string {
    return (getA2aTaskStatus(taskResponse).message?.parts ?? []).map((part) => part.text ?? '').join('');
}
