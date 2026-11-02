import {type BrowserContext, type Page, type Route, expect} from '@playwright/test';
import {randomUUID} from 'node:crypto';
import {type Server, createServer} from 'node:http';

export const COPILOT_WORKFLOW_EDITOR_CHAT_URL = /\/api\/platform\/internal\/ai\/chat\/workflow_editor(\?.*)?$/;

export const AI_HUB_CHAT_URL = /\/api\/platform\/internal\/ai\/chat\/ai_hub(\?.*)?$/;

export const AI_HUB_ATTACH_URL = /\/api\/platform\/internal\/ai\/chat\/ai_hub\/[^/?]+\/attach(\?.*)?$/;

export interface AgUiRequestMessageI {
    content?: string;
    id: string;
    role: string;
}

export interface AgUiRequestI {
    context?: unknown[];
    forwardedProps?: unknown;
    messages: AgUiRequestMessageI[];
    runId: string;
    state: Record<string, unknown>;
    threadId: string;
    tools?: Array<{name: string}>;
}

export interface AgUiRunContextI {
    runId: string;
    threadId: string;
}

export type AgUiWireEventType = Record<string, unknown> & {type: string};

export type AgUiEventType = (runContext: AgUiRunContextI) => AgUiWireEventType[];

export interface AgUiGateI {
    kind: 'gate';
    release: () => void;
    released: Promise<void>;
}

export type AgUiTurnStepType = AgUiEventType | AgUiGateI;

export type AgUiTurnType = AgUiTurnStepType[];

export interface AgUiRejectedRunI {
    body: unknown;
    kind: 'rejected';
    status: number;
}

export type AgUiStubTurnType = AgUiTurnType | AgUiRejectedRunI;

export interface AgUiStubI {
    requests: AgUiRequestI[];
    waitForRequest: (requestIndex: number) => Promise<AgUiRequestI>;
}

const wireEvent = (type: string, fields: Record<string, unknown>): AgUiWireEventType => ({
    ...fields,
    rawEvent: null,
    timestamp: Date.now(),
    type,
});

export const runStarted = (): AgUiEventType => (runContext) => [
    wireEvent('RUN_STARTED', {runId: runContext.runId, threadId: runContext.threadId}),
];

export const runFinished = (): AgUiEventType => (runContext) => [
    wireEvent('RUN_FINISHED', {result: null, runId: runContext.runId, threadId: runContext.threadId}),
];

export const runError =
    (message: string, code?: string): AgUiEventType =>
    () => [wireEvent('RUN_ERROR', code === undefined ? {message} : {code, message})];

export const textMessage =
    (text: string, messageId: string = randomUUID()): AgUiEventType =>
    () => [
        wireEvent('TEXT_MESSAGE_START', {messageId, role: 'assistant'}),
        wireEvent('TEXT_MESSAGE_CONTENT', {delta: text, messageId}),
        wireEvent('TEXT_MESSAGE_END', {messageId}),
    ];

interface ToolCallOptionsI {
    parentMessageId?: string;
    toolCallId?: string;
}

interface ToolCallIdsI {
    parentMessageId: string;
    toolCallId: string;
}

export const toolCallStarted =
    (toolCallName: string, toolCallArguments: Record<string, unknown>, {parentMessageId, toolCallId}: ToolCallIdsI) =>
    (): AgUiWireEventType[] => [
        wireEvent('TOOL_CALL_START', {parentMessageId, toolCallId, toolCallName}),
        wireEvent('TOOL_CALL_ARGS', {delta: JSON.stringify(toolCallArguments), toolCallId}),
        wireEvent('TOOL_CALL_END', {toolCallId}),
    ];

export const toolCallResult =
    (resultContent: string, {parentMessageId, toolCallId}: ToolCallIdsI) =>
    (): AgUiWireEventType[] => [
        wireEvent('TOOL_CALL_RESULT', {content: resultContent, messageId: parentMessageId, role: 'tool', toolCallId}),
    ];

export const toolCall = (
    toolCallName: string,
    toolCallArguments: Record<string, unknown>,
    resultContent: string,
    {parentMessageId = randomUUID(), toolCallId = randomUUID()}: ToolCallOptionsI = {}
): AgUiEventType => {
    const toolCallIds = {parentMessageId, toolCallId};

    return () => [
        ...toolCallStarted(toolCallName, toolCallArguments, toolCallIds)(),
        ...toolCallResult(resultContent, toolCallIds)(),
    ];
};

export const newToolCallIds = (): ToolCallIdsI => ({parentMessageId: randomUUID(), toolCallId: randomUUID()});

export function createAgUiGate(): AgUiGateI {
    let release: () => void = () => undefined;

    const released = new Promise<void>((resolve) => {
        release = resolve;
    });

    return {kind: 'gate', release, released};
}

export const completedTurn = (...events: AgUiTurnStepType[]): AgUiTurnType => [runStarted(), ...events, runFinished()];

export const failedTurn = (message: string, code?: string): AgUiTurnType => [runStarted(), runError(message, code)];

export const rejectedRun = (status: number, body: unknown): AgUiRejectedRunI => ({body, kind: 'rejected', status});

const isGate = (turnStep: AgUiTurnStepType): turnStep is AgUiGateI =>
    typeof turnStep === 'object' && turnStep.kind === 'gate';

const isRejectedRun = (turn: AgUiStubTurnType): turn is AgUiRejectedRunI => !Array.isArray(turn);

const toSseChunk = (turnEvent: AgUiEventType, runContext: AgUiRunContextI) =>
    turnEvent(runContext)
        .map((wireEventItem) => `data: ${JSON.stringify(wireEventItem)}\n\n`)
        .join('');

export function toSseBody(turn: AgUiTurnType, runContext: AgUiRunContextI): string {
    return turn
        .map((turnStep) => {
            if (isGate(turnStep)) {
                throw new Error('A gated turn cannot be served as one response body');
            }

            return toSseChunk(turnStep, runContext);
        })
        .join('');
}

interface GatedStreamI {
    runContext: AgUiRunContextI;
    turn: AgUiTurnType;
}

/**
 * Serves gated turns from a local HTTP server, so the events before a gate reach the page while the turn stays open,
 * the way a running tool holds the real server's stream. The page's request is re-pointed at this server with
 * `route.continue`, which keeps the response streaming instead of buffering it like `route.fulfill` does.
 */
class GatedStreamServer {
    private readonly gatedStreams = new Map<string, GatedStreamI>();

    private server: Server | undefined;

    private serverOrigin: Promise<string> | undefined;

    async register(gatedStream: GatedStreamI): Promise<string> {
        const streamToken = randomUUID();

        this.gatedStreams.set(streamToken, gatedStream);

        return `${await this.origin()}/${streamToken}`;
    }

    close(): void {
        this.server?.closeAllConnections();
        this.server?.close();
    }

    private origin(): Promise<string> {
        if (this.serverOrigin === undefined) {
            const server = createServer(async (request, response) => {
                const gatedStream = this.gatedStreams.get((request.url ?? '').slice(1));

                if (gatedStream === undefined) {
                    response.writeHead(404).end();

                    return;
                }

                let clientGone = false;

                response.on('close', () => {
                    clientGone = true;
                });

                response.writeHead(200, {
                    'Access-Control-Allow-Origin': '*',
                    'Cache-Control': 'no-cache',
                    'Content-Type': 'text/event-stream',
                });

                for (const turnStep of gatedStream.turn) {
                    if (clientGone) {
                        return;
                    }

                    if (isGate(turnStep)) {
                        await turnStep.released;

                        continue;
                    }

                    response.write(toSseChunk(turnStep, gatedStream.runContext));
                }

                response.end();
            });

            this.server = server;

            this.serverOrigin = new Promise((resolve) => {
                server.listen(0, '127.0.0.1', () => {
                    const address = server.address();

                    resolve(`http://127.0.0.1:${typeof address === 'object' && address !== null ? address.port : 0}`);
                });
            });
        }

        return this.serverOrigin;
    }
}

type RouteTargetType = BrowserContext | Page;

/**
 * Stubs an AG-UI agent endpoint: each POST gets the next scripted turn and its body is recorded. A turn is served as
 * one `text/event-stream` body, streamed through a local server when it holds a gate, or answered with a plain status
 * and JSON body when it is a rejected run. Pass the browser context instead of a page to serve every tab.
 */
export async function stubAgUiRun(
    routeTarget: RouteTargetType,
    agentIdPattern: RegExp,
    turns: AgUiStubTurnType[]
): Promise<AgUiStubI> {
    const gatedStreamServer = new GatedStreamServer();
    const requests: AgUiRequestI[] = [];

    (routeTarget as Pick<Page, 'once'>).once('close', () => gatedStreamServer.close());

    await routeTarget.route(agentIdPattern, async (route: Route) => {
        const request = route.request();

        if (request.method() !== 'POST') {
            await route.fallback();

            return;
        }

        const agUiRequest = request.postDataJSON() as AgUiRequestI;

        requests.push(agUiRequest);

        const turn = turns[requests.length - 1];

        if (!turn) {
            await route.fulfill({
                body: `No stubbed AG-UI turn for request ${requests.length}`,
                status: 500,
            });

            return;
        }

        if (isRejectedRun(turn)) {
            await route.fulfill({json: turn.body, status: turn.status});

            return;
        }

        const runContext = {runId: agUiRequest.runId, threadId: agUiRequest.threadId};

        if (turn.some(isGate)) {
            await route.continue({url: await gatedStreamServer.register({runContext, turn})});

            return;
        }

        await route.fulfill({
            body: toSseBody(turn, runContext),
            contentType: 'text/event-stream',
            headers: {'Cache-Control': 'no-cache'},
            status: 200,
        });
    });

    const waitForRequest = async (requestIndex: number) => {
        await expect.poll(() => requests.length).toBeGreaterThan(requestIndex);

        return requests[requestIndex];
    };

    return {requests, waitForRequest};
}

export const stubCopilotRun = stubAgUiRun;

export function lastMessageOf(agUiRequest: AgUiRequestI): AgUiRequestMessageI | undefined {
    return agUiRequest.messages.at(-1);
}
