import {type APIRequestContext, type APIResponse, expect} from '@playwright/test';

import {resumeJob} from './approvalUtils';

export const CHAT_STREAM_TIMEOUT = 180000;

export interface SseEventI {
    data: string;
    event: string;
}

export interface ApprovalRequestEventDataI {
    expiresAt?: string;
    formDescription?: string;
    formTitle?: string;
    formUrl?: string;
    inputs?: unknown[];
    resumeId: string;
    resumeUrl?: string;
    [key: string]: unknown;
}

export function parseSseEvents(streamBody: string): SseEventI[] {
    return streamBody
        .split(/\r?\n\r?\n/)
        .map((eventBlock) => {
            const eventLines = eventBlock.split(/\r?\n/);

            const eventName = eventLines.find((eventLine) => eventLine.startsWith('event:'))?.slice('event:'.length);

            const eventData = eventLines
                .filter((eventLine) => eventLine.startsWith('data:'))
                .map((eventLine) => eventLine.slice('data:'.length))
                .join('\n');

            return {data: eventData, event: (eventName ?? '').trim()};
        })
        .filter((sseEvent) => sseEvent.event !== '');
}

export function getEventNames(sseEvents: SseEventI[]): string[] {
    return sseEvents.map((sseEvent) => sseEvent.event);
}

export function getApprovalRequests(sseEvents: SseEventI[]): ApprovalRequestEventDataI[] {
    return sseEvents
        .filter((sseEvent) => sseEvent.event === 'approval_request')
        .map((sseEvent) => JSON.parse(sseEvent.data) as ApprovalRequestEventDataI);
}

export function getResultMessage(sseEvents: SseEventI[]): string {
    const resultEvent = sseEvents.find((sseEvent) => sseEvent.event === 'result');

    if (!resultEvent) {
        return '';
    }

    try {
        const resultData = JSON.parse(resultEvent.data) as {message?: unknown};

        return typeof resultData.message === 'string' ? resultData.message : JSON.stringify(resultData);
    } catch {
        return resultEvent.data;
    }
}

export function getStartedJobId(sseEvents: SseEventI[]): string {
    const startEvent = sseEvents.find((sseEvent) => sseEvent.event === 'start');

    expect(startEvent).toBeTruthy();

    return String((JSON.parse(startEvent!.data) as {jobId: string | number}).jobId);
}

export interface ChatStreamI {
    events: SseEventI[];
    response: APIResponse;
}

/**
 * Sends one chat message to a deployed chat-trigger workflow over its SSE endpoint. The server closes the stream
 * after `suspended` or `result`, so the whole stream is read at once.
 */
export async function sendChatMessage({
    conversationId,
    message,
    request,
    webhookUrl,
}: {
    conversationId: string;
    message: string;
    request: APIRequestContext;
    webhookUrl: string;
}): Promise<ChatStreamI> {
    const response = await request.post(`${webhookUrl}/sse`, {
        data: JSON.stringify({conversationId, message}),
        headers: {Accept: 'text/event-stream', 'Content-Type': 'application/json'},
        timeout: CHAT_STREAM_TIMEOUT,
    });

    return {events: parseSseEvents(await response.text()), response};
}

export async function resumeJobStreaming({
    body,
    request,
    token,
}: {
    body: Record<string, unknown>;
    request: APIRequestContext;
    token: string;
}): Promise<ChatStreamI> {
    const response = await resumeJob({accept: 'text/event-stream', body, request, token});

    return {events: parseSseEvents(await response.text()), response};
}
