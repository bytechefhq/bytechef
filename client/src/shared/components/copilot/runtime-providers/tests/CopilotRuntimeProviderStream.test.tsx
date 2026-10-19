import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {AppendMessage, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {act, render} from '@testing-library/react';
import {readFileSync} from 'node:fs';
import {resolve} from 'node:path';
import {ReactNode, StrictMode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {CopilotRuntimeProvider} from '../CopilotRuntimeProvider';

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    AuiConfig: vi.fn(),
    Suggestions: vi.fn(),
    useExternalStoreRuntime: vi.fn(() => ({})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiProviderCatalogQuery: vi.fn(() => ({data: undefined})),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn()},
}));

const capturedBuildStream = readFileSync(resolve(__dirname, 'fixtures/copilot-build-stream.sse'), 'utf8');

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        isRunning: boolean;
        messages: ThreadMessageLike[];
        onNew: (message: AppendMessage) => Promise<void>;
    };
}

function toStreamResponse(body: string) {
    const encodedBody = new TextEncoder().encode(body);

    return new Response(
        new ReadableStream<Uint8Array>({
            start(controller) {
                controller.enqueue(encodedBody);
                controller.close();
            },
        }),
        {headers: {'Content-Type': 'text/event-stream'}, status: 200}
    );
}

const partTypesOf = (message: ThreadMessageLike) =>
    typeof message.content === 'string' ? ['text'] : message.content.map((part) => (part as {type: string}).type);

describe('CopilotRuntimeProvider with the captured build stream', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
            conversationId: 'conversation-1',
            messages: [],
            selectedLlmModel: null,
            selectedLlmProvider: null,
        });

        vi.spyOn(globalThis, 'fetch').mockImplementation(async () => toStreamResponse(capturedBuildStream));
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('renders the assistant text, the tool results and the ask-user-question card', async () => {
        render(
            <StrictMode>
                <CopilotRuntimeProvider>child</CopilotRuntimeProvider>
            </StrictMode>
        );

        await act(async () => {
            await getRuntimeOptions().onNew({
                content: [{text: 'Build this workflow', type: 'text'}],
            } as unknown as AppendMessage);
        });

        const renderedMessages = getRuntimeOptions().messages;
        const assistantMessages = renderedMessages.filter((message) => message.role === 'assistant');
        const assistantPartTypes = assistantMessages.flatMap(partTypesOf);

        expect(renderedMessages[0]).toEqual({content: 'Build this workflow', id: expect.any(String), role: 'user'});
        expect(assistantPartTypes).toContain('text');
        expect(assistantPartTypes).toContain('data-ask-user-question');
        expect(
            assistantMessages.some(
                (message) =>
                    typeof message.content === 'string' && message.content.startsWith("I'll build this workflow")
            )
        ).toBe(true);
    });
});
