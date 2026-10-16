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

        addMessage = (message: AgentMessageI) => {
            this.messages = [...this.messages, message];
        };

        runAgent = vi.fn(async () => {});

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

vi.mock('sonner', () => ({
    toast: {error: vi.fn()},
}));

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        onEdit: (message: AppendMessage) => Promise<void>;
        onReload: (parentId: string | null) => Promise<void>;
    };
}

function getAgent() {
    return agents[agents.length - 1];
}

const user = (content: string): ThreadMessageLike => ({content, role: 'user'});
const assistant = (content: string): ThreadMessageLike => ({content, role: 'assistant'});

const agentUser = (content: string): AgentMessageI => ({content, id: content, role: 'user'});
const agentAssistant = (id: string): AgentMessageI => ({id, role: 'assistant'});
const agentTool = (id: string): AgentMessageI => ({id, role: 'tool'});

describe('CopilotRuntimeProvider', () => {
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
            ],
        });

        render(<CopilotRuntimeProvider>child</CopilotRuntimeProvider>);

        getAgent().messages = [
            agentUser('first'),
            agentAssistant('a1'),
            agentUser('second'),
            agentAssistant('a2'),
            agentTool('t1'),
            agentAssistant('a3'),
        ];
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('regenerates the whole turn when refreshing one of its middle messages', async () => {
        await act(async () => {
            await getRuntimeOptions().onReload('3');
        });

        expect(useCopilotStore.getState().messages).toEqual([user('first'), assistant('first reply'), user('second')]);
        expect(getAgent().messages.map((message) => message.id)).toEqual(['first', 'a1', 'second']);
        expect(getAgent().runAgent).toHaveBeenCalledTimes(1);
    });

    it('does not refresh when the turn cannot be resolved', async () => {
        await act(async () => {
            await getRuntimeOptions().onReload(null);
        });

        expect(toast.error).toHaveBeenCalledWith('Could not resolve message to refresh');
        expect(useCopilotStore.getState().messages).toHaveLength(5);
        expect(getAgent().runAgent).not.toHaveBeenCalled();
    });

    it('cuts the agent transcript at the edited user message', async () => {
        await act(async () => {
            await getRuntimeOptions().onEdit({
                content: [{text: 'second, edited', type: 'text'}],
                sourceId: '2',
            } as unknown as AppendMessage);
        });

        expect(useCopilotStore.getState().messages).toEqual([
            user('first'),
            assistant('first reply'),
            user('second, edited'),
        ]);
        expect(getAgent().messages.map((message) => message.content ?? message.id)).toEqual([
            'first',
            'a1',
            'second, edited',
        ]);
        expect(getAgent().runAgent).toHaveBeenCalledTimes(1);
    });

    it('does not edit when the agent transcript does not reach back to the message', async () => {
        getAgent().messages = [agentUser('second'), agentAssistant('a2')];

        await act(async () => {
            await getRuntimeOptions().onEdit({
                content: [{text: 'first, edited', type: 'text'}],
                sourceId: '0',
            } as unknown as AppendMessage);
        });

        expect(toast.error).toHaveBeenCalledWith('Could not resolve edited message');
        expect(useCopilotStore.getState().messages[0]).toEqual(user('first'));
        expect(getAgent().runAgent).not.toHaveBeenCalled();
    });
});
