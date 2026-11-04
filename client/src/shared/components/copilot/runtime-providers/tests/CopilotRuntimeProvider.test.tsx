import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {AppendMessage, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {toast} from 'sonner';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {CopilotRuntimeProvider} from '../CopilotRuntimeProvider';

interface AgentMessageI {
    content?: string;
    id: string;
    role: string;
}

const {FakeHttpAgent, agents} = vi.hoisted(() => {
    class FakeHttpAgent {
        messages: AgentMessageI[] = [];

        sentMessages: AgentMessageI[][] = [];

        abortRun = vi.fn();

        addMessage = (message: AgentMessageI) => {
            this.messages = [...this.messages, message];
        };

        runAgent = vi.fn(async () => {
            this.sentMessages.push(this.messages);
        });

        setMessages = (messages: AgentMessageI[]) => {
            this.messages = messages;
        };

        setState = vi.fn();

        constructor() {
            agents.push(this);
        }
    }

    const agents: FakeHttpAgent[] = [];

    return {FakeHttpAgent, agents};
});

vi.mock('@ag-ui/client', () => ({
    HttpAgent: FakeHttpAgent,
    contentToText: (content: unknown) => content,
}));

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

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        onEdit: (message: AppendMessage) => Promise<void>;
        onNew: (message: AppendMessage) => Promise<void>;
        onReload: (parentId: string | null) => Promise<void>;
    };
}

function getAgent() {
    return agents[agents.length - 1];
}

function getStoreTexts() {
    return useCopilotStore.getState().messages.map((message) => message.content);
}

function getSentTexts() {
    return getAgent()
        .sentMessages.at(-1)!
        .map((message) => `${message.role}: ${message.content}`);
}

function editMessage(sourceId: string, text: string) {
    return getRuntimeOptions().onEdit({
        content: [{text, type: 'text'}],
        sourceId,
    } as unknown as AppendMessage);
}

const user = (content: string, id?: string): ThreadMessageLike => ({content, id, role: 'user'});
const assistant = (content: string, id?: string): ThreadMessageLike => ({content, id, role: 'assistant'});

const fallbackId = (index: number) => `__external_store_fallback_${index}`;

describe('CopilotRuntimeProvider', () => {
    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('with messages that have no id', () => {
        beforeEach(() => {
            agents.length = 0;

            useCopilotStore.setState({
                conversationId: 'conversation-1',
                messages: [
                    user('first'),
                    assistant('first reply'),
                    user('second'),
                    assistant('Looking it up.'),
                    assistant('Found it.'),
                    user('third'),
                    assistant('third reply'),
                ],
            });

            render(<CopilotRuntimeProvider>child</CopilotRuntimeProvider>);
        });

        it('replaces an edited middle message and drops everything after it', async () => {
            await act(async () => {
                await editMessage(fallbackId(2), 'second, edited');
            });

            expect(getStoreTexts()).toEqual(['first', 'first reply', 'second, edited']);
            expect(getSentTexts()).toEqual(['user: first', 'assistant: first reply', 'user: second, edited']);
            expect(getAgent().runAgent).toHaveBeenCalledTimes(1);
        });

        it('regenerates the last reply for its user message', async () => {
            await act(async () => {
                await getRuntimeOptions().onReload(fallbackId(5));
            });

            expect(getStoreTexts()).toEqual(['first', 'first reply', 'second', 'Looking it up.', 'Found it.', 'third']);
            expect(getSentTexts().at(-1)).toBe('user: third');
            expect(getAgent().runAgent).toHaveBeenCalledTimes(1);
        });

        it('regenerates the whole turn when refreshing one of its middle messages', async () => {
            await act(async () => {
                await getRuntimeOptions().onReload(fallbackId(3));
            });

            expect(getStoreTexts()).toEqual(['first', 'first reply', 'second']);
            expect(getAgent().runAgent).toHaveBeenCalledTimes(1);
        });

        it('does not refresh when the turn cannot be resolved', async () => {
            await act(async () => {
                await getRuntimeOptions().onReload(null);
            });

            expect(toast.error).toHaveBeenCalledWith('Could not resolve message to refresh');
            expect(useCopilotStore.getState().messages).toHaveLength(7);
            expect(getAgent().runAgent).not.toHaveBeenCalled();
        });

        it('does not edit a message it cannot resolve', async () => {
            await act(async () => {
                await editMessage(fallbackId(42), 'edited');
            });

            expect(toast.error).toHaveBeenCalledWith('Could not resolve edited message');
            expect(useCopilotStore.getState().messages).toHaveLength(7);
            expect(getAgent().runAgent).not.toHaveBeenCalled();
        });

        it('does not edit an assistant message', async () => {
            await act(async () => {
                await editMessage(fallbackId(1), 'edited');
            });

            expect(toast.error).toHaveBeenCalledWith('Could not resolve edited message');
            expect(getAgent().runAgent).not.toHaveBeenCalled();
        });
    });

    describe('with messages that have ids', () => {
        beforeEach(() => {
            agents.length = 0;

            useCopilotStore.setState({
                conversationId: 'conversation-1',
                messages: [
                    user('first', 'user-1'),
                    assistant('first reply', 'assistant-1'),
                    user('second', 'user-2'),
                    assistant('second reply', 'assistant-2'),
                ],
            });

            render(<CopilotRuntimeProvider>child</CopilotRuntimeProvider>);
        });

        it('replaces an edited message by its id and keeps that id', async () => {
            await act(async () => {
                await editMessage('user-1', 'first, edited');
            });

            expect(useCopilotStore.getState().messages).toEqual([user('first, edited', 'user-1')]);
            expect(getAgent().sentMessages.at(-1)).toEqual([{content: 'first, edited', id: 'user-1', role: 'user'}]);
        });

        it('regenerates the last reply by its parent id', async () => {
            await act(async () => {
                await getRuntimeOptions().onReload('user-2');
            });

            expect(getStoreTexts()).toEqual(['first', 'first reply', 'second']);
            expect(getAgent().runAgent).toHaveBeenCalledTimes(1);
        });

        it('keeps the edited user message when the re-run fails', async () => {
            getAgent().runAgent.mockRejectedValueOnce(new Error('network down'));

            await act(async () => {
                await editMessage('user-2', 'second, edited');
            });

            expect(useCopilotStore.getState().messages).toEqual([
                user('first', 'user-1'),
                assistant('first reply', 'assistant-1'),
                user('second, edited', 'user-2'),
            ]);
            expect(toast.error).toHaveBeenCalledWith('Failed to update message: network down');
        });

        it('keeps the user message when a regenerated reply fails', async () => {
            getAgent().runAgent.mockRejectedValueOnce(new Error('network down'));

            await act(async () => {
                await getRuntimeOptions().onReload('user-2');
            });

            expect(useCopilotStore.getState().messages.at(-1)).toEqual(user('second', 'user-2'));
            expect(toast.error).toHaveBeenCalledWith('Failed to refresh response: network down');
        });

        it('sends the whole visible conversation even when the agent was rebuilt', async () => {
            await act(async () => {
                await getRuntimeOptions().onNew({content: [{text: 'third', type: 'text'}]} as unknown as AppendMessage);
            });

            expect(getSentTexts()).toEqual([
                'user: first',
                'assistant: first reply',
                'user: second',
                'assistant: second reply',
                'user: third',
            ]);
            expect(useCopilotStore.getState().messages.at(-1)?.id).toEqual(expect.any(String));
        });
    });
});
