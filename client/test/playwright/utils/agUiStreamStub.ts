import {type Page, type Route} from '@playwright/test';
import {randomUUID} from 'node:crypto';

export const COPILOT_WORKFLOW_EDITOR_CHAT_URL = /\/api\/platform\/internal\/ai\/chat\/workflow_editor(\?.*)?$/;

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

export type AgUiTurnType = AgUiEventType[];

export interface AgUiStubI {
    requests: AgUiRequestI[];
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

export const toolCall =
    (
        toolCallName: string,
        toolCallArguments: Record<string, unknown>,
        resultContent: string,
        {parentMessageId = randomUUID(), toolCallId = randomUUID()}: ToolCallOptionsI = {}
    ): AgUiEventType =>
    () => [
        wireEvent('TOOL_CALL_START', {parentMessageId, toolCallId, toolCallName}),
        wireEvent('TOOL_CALL_ARGS', {delta: JSON.stringify(toolCallArguments), toolCallId}),
        wireEvent('TOOL_CALL_END', {toolCallId}),
        wireEvent('TOOL_CALL_RESULT', {content: resultContent, messageId: parentMessageId, role: 'tool', toolCallId}),
    ];

export const completedTurn = (...events: AgUiEventType[]): AgUiTurnType => [runStarted(), ...events, runFinished()];

export const failedTurn = (message: string, code?: string): AgUiTurnType => [runStarted(), runError(message, code)];

export function toSseBody(turn: AgUiTurnType, runContext: AgUiRunContextI): string {
    return turn
        .flatMap((event) => event(runContext))
        .map((wireEventItem) => `data: ${JSON.stringify(wireEventItem)}\n\n`)
        .join('');
}

export async function stubCopilotRun(page: Page, agentIdPattern: RegExp, turns: AgUiTurnType[]): Promise<AgUiStubI> {
    const requests: AgUiRequestI[] = [];

    await page.route(agentIdPattern, async (route: Route) => {
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

        await route.fulfill({
            body: toSseBody(turn, {runId: agUiRequest.runId, threadId: agUiRequest.threadId}),
            contentType: 'text/event-stream',
            headers: {'Cache-Control': 'no-cache'},
            status: 200,
        });
    });

    return {requests};
}

export function lastMessageOf(agUiRequest: AgUiRequestI): AgUiRequestMessageI | undefined {
    return agUiRequest.messages.at(-1);
}
