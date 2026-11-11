import useFetchInterceptor, {clearActiveToasts} from '@/config/useFetchInterceptor';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {toast} from 'sonner';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubRuntimeProvider} from '../AiHubRuntimeProvider';

const hoisted = vi.hoisted(() => ({
    registeredHandlers: null as {response: (response: Response) => Response} | null,
    setComposerText: vi.fn(),
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useExternalStoreRuntime: vi.fn(() => ({thread: {composer: {setText: hoisted.setComposerText}}})),
}));

vi.mock('fetch-intercept', () => ({
    default: {
        register: vi.fn((handlers: typeof hoisted.registeredHandlers) => {
            hoisted.registeredHandlers = handlers;

            return vi.fn();
        }),
    },
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: vi.fn(), info: vi.fn()},
}));

const FetchInterceptor = () => {
    useFetchInterceptor();

    return null;
};

function getRuntimeOptions() {
    return vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
        onNew: (message: AppendMessage) => Promise<void>;
    };
}

const toGraphQlResponse = (body: unknown, status = 200) => {
    const response = new Response(JSON.stringify(body), {headers: {'Content-Type': 'application/json'}, status});

    Object.defineProperty(response, 'url', {value: 'http://localhost/graphql'});

    return response;
};

const respondToGraphQl = (respond: () => Response) =>
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
        if (!String(input).includes('/graphql')) {
            throw new Error('offline');
        }

        return hoisted.registeredHandlers!.response(respond());
    });

async function sendFirstMessage() {
    render(
        <QueryClientProvider client={new QueryClient()}>
            <FetchInterceptor />

            <AiHubRuntimeProvider>child</AiHubRuntimeProvider>
        </QueryClientProvider>
    );

    await act(async () => {
        await getRuntimeOptions().onNew({
            content: [{text: 'Build me a workflow', type: 'text'}],
        } as unknown as AppendMessage);
    });

    await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
    });
}

describe('AiHubRuntimeProvider when the first message cannot create the chat', () => {
    beforeEach(() => {
        clearActiveToasts();

        useWorkspaceStore.setState({currentWorkspaceId: 1});

        aiHubRunStateStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubStore.setState({chatId: 'thread-1', messages: []});
        aiChatRetryableErrorStore.setState({currentError: undefined});
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('reports a GraphQL error once, in the global toast, and gives the message back to the composer', async () => {
        respondToGraphQl(() =>
            toGraphQlResponse({
                data: null,
                errors: [{extensions: {errorCode: 'INTERNAL'}, message: 'database down', path: ['createAiHubChat']}],
            })
        );

        await sendFirstMessage();

        expect(toast.error).toHaveBeenCalledTimes(1);
        expect(toast.error).toHaveBeenCalledWith('Error', expect.objectContaining({description: 'database down'}));
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(hoisted.setComposerText).toHaveBeenCalledWith('Build me a workflow');
        expect(aiHubStore.getState().chatId).toBe('thread-1');
    });

    it('reports a non-2xx response once, in the global toast', async () => {
        respondToGraphQl(() => toGraphQlResponse({}, 500));

        await sendFirstMessage();

        expect(toast.error).toHaveBeenCalledTimes(1);
        expect(toast.error).toHaveBeenCalledWith('Request failed with status 500', expect.anything());
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });

    it('starts a fresh thread when the thread id already belongs to another chat, so a resend does not conflict', async () => {
        respondToGraphQl(() =>
            toGraphQlResponse({
                data: null,
                errors: [
                    {
                        extensions: {errorCode: 'CONFLICT'},
                        message: 'AiHubChat thread-1 already exists',
                        path: ['createAiHubChat'],
                    },
                ],
            })
        );

        await sendFirstMessage();

        expect(toast.error).toHaveBeenCalledTimes(1);
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(aiHubStore.getState().chatId).not.toBe('thread-1');
        expect(hoisted.setComposerText).toHaveBeenCalledWith('Build me a workflow');
    });

    it('reports a network failure once, in its own toast', async () => {
        vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));

        await sendFirstMessage();

        expect(toast.error).toHaveBeenCalledTimes(1);
        expect(toast.error).toHaveBeenCalledWith('Failed to start chat: Failed to fetch');
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(hoisted.setComposerText).toHaveBeenCalledWith('Build me a workflow');
    });
});
