import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {act, render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {CopilotRuntimeProvider} from './CopilotRuntimeProvider';

interface FakeSubscriberI {
    onRunErrorEvent?: (params: {event: {code?: string; message?: string}}) => void;
    onTextMessageContentEvent?: (params: {event: {delta: string}; textMessageBuffer: string}) => void;
}

interface FakeAgentI {
    abortRun: ReturnType<typeof vi.fn>;
    rejectRun?: (error: Error) => void;
    resolveRun?: () => void;
    setState: ReturnType<typeof vi.fn>;
    subscriber?: FakeSubscriberI;
}

interface RuntimeOptionsI {
    isRunning: boolean;
    onNew: (message: {content: {text: string; type: string}[]}) => Promise<void>;
}

const hoisted = vi.hoisted(() => ({
    agents: [] as FakeAgentI[],
    providerCatalog: undefined as unknown,
    runtimeOptions: undefined as RuntimeOptionsI | undefined,
    toastError: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastError},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiProviderCatalogQuery: () => ({
        data: hoisted.providerCatalog === undefined ? undefined : {aiProviderCatalog: hoisted.providerCatalog},
    }),
}));

vi.mock('@ag-ui/client', () => ({
    HttpAgent: class {
        abortRun = vi.fn();
        addMessage = vi.fn();
        messages = [];
        rejectRun?: (error: Error) => void;
        resolveRun?: () => void;
        setMessages = vi.fn();
        setState = vi.fn();
        subscriber?: FakeSubscriberI;

        constructor() {
            hoisted.agents.push(this as unknown as FakeAgentI);
        }

        runAgent(_input: unknown, subscriber: FakeSubscriberI) {
            this.subscriber = subscriber;

            return new Promise<void>((resolve, reject) => {
                this.rejectRun = reject;
                this.resolveRun = resolve;
            });
        }
    },
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: React.ReactNode}) => children,
    AuiConfig: () => ({}),
    Suggestions: () => ({}),
    useExternalStoreRuntime: (options: RuntimeOptionsI) => {
        hoisted.runtimeOptions = options;

        return {};
    },
}));

describe('CopilotRuntimeProvider', () => {
    beforeEach(() => {
        hoisted.agents.length = 0;
        hoisted.providerCatalog = undefined;
        hoisted.runtimeOptions = undefined;

        localStorage.clear();

        useCopilotPostTurnRegistry.setState({callbacks: {}});
        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
            conversationId: 'conversation-1',
            messages: [],
            selectedLlmModel: null,
            selectedLlmProvider: null,
        });
        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    const renderProvider = () =>
        render(
            <CopilotRuntimeProvider>
                <div />
            </CopilotRuntimeProvider>
        );

    const startSend = () => {
        let sendPromise: Promise<void> | undefined;

        act(() => {
            sendPromise = hoisted.runtimeOptions?.onNew({content: [{text: 'hello', type: 'text'}]});
        });

        return sendPromise;
    };

    const finishRun = async (agent: FakeAgentI, sendPromise: Promise<void> | undefined) => {
        await act(async () => {
            agent.resolveRun?.();

            await sendPromise;
        });
    };

    it('aborts the in-flight run when the conversation changes', () => {
        render(
            <CopilotRuntimeProvider>
                <div />
            </CopilotRuntimeProvider>
        );

        const firstAgent = hoisted.agents[0];

        act(() => useCopilotStore.setState({conversationId: 'conversation-2'}));

        expect(firstAgent.abortRun).toHaveBeenCalled();
    });

    it('aborts the in-flight run on unmount', () => {
        const {unmount} = render(
            <CopilotRuntimeProvider>
                <div />
            </CopilotRuntimeProvider>
        );

        unmount();

        expect(hoisted.agents[0].abortRun).toHaveBeenCalled();
    });

    it('drops streamed text and post-turn callbacks of a run whose conversation was replaced', async () => {
        const postTurnCallback = vi.fn();

        useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_EDITOR, postTurnCallback);

        render(
            <CopilotRuntimeProvider>
                <div />
            </CopilotRuntimeProvider>
        );

        const agent = hoisted.agents[0];

        let sendPromise: Promise<void> | undefined;

        act(() => {
            sendPromise = hoisted.runtimeOptions?.onNew({content: [{text: 'hello', type: 'text'}]});
        });

        const restoredMessages = [
            {content: 'earlier question', role: 'user' as const},
            {content: 'earlier answer', role: 'assistant' as const},
        ];

        act(() => useCopilotStore.setState({conversationId: 'restored-conversation', messages: restoredMessages}));

        act(() => agent.subscriber?.onTextMessageContentEvent?.({event: {delta: 'late reply'}, textMessageBuffer: ''}));

        await act(async () => {
            agent.resolveRun?.();

            await sendPromise;
        });

        expect(useCopilotStore.getState().messages).toEqual(restoredMessages);
        expect(postTurnCallback).not.toHaveBeenCalled();
    });

    it('streams text and runs post-turn callbacks for the current conversation', async () => {
        const postTurnCallback = vi.fn();

        useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_EDITOR, postTurnCallback);

        render(
            <CopilotRuntimeProvider>
                <div />
            </CopilotRuntimeProvider>
        );

        const agent = hoisted.agents[0];

        let sendPromise: Promise<void> | undefined;

        act(() => {
            sendPromise = hoisted.runtimeOptions?.onNew({content: [{text: 'hello', type: 'text'}]});
        });

        act(() => useCopilotStore.getState().addMessage({content: '', role: 'assistant'}));

        act(() => agent.subscriber?.onTextMessageContentEvent?.({event: {delta: 'reply'}, textMessageBuffer: ''}));

        await act(async () => {
            agent.resolveRun?.();

            await sendPromise;
        });

        expect(useCopilotStore.getState().messages.at(-1)).toMatchObject({content: 'reply', role: 'assistant'});
        expect(postTurnCallback).toHaveBeenCalled();
    });

    describe('run errors', () => {
        it('renders the humanised RUN_ERROR message and still runs post-turn callbacks', async () => {
            const postTurnCallback = vi.fn();

            useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_EDITOR, postTurnCallback);

            renderProvider();

            const agent = hoisted.agents[0];
            const sendPromise = startSend();

            act(() =>
                agent.subscriber?.onRunErrorEvent?.({
                    event: {message: 'java.lang.IllegalStateException: Model gpt-x is not available'},
                })
            );

            await finishRun(agent, sendPromise);

            expect(useCopilotStore.getState().messages.at(-1)).toEqual({
                content: [{data: {message: 'Model gpt-x is not available'}, type: 'data-run-error'}],
                id: expect.any(String),
                role: 'assistant',
            });
            expect(postTurnCallback).toHaveBeenCalledTimes(1);
        });

        it('renders the next step for a RUN_ERROR that carries a known code', async () => {
            renderProvider();

            const agent = hoisted.agents[0];
            const sendPromise = startSend();

            act(() =>
                agent.subscriber?.onRunErrorEvent?.({
                    event: {
                        code: 'MODEL_SELECTION_INCOMPLETE',
                        message: 'The model selection is incomplete (provider=openai, model=null). Pick a model again.',
                    },
                })
            );

            await finishRun(agent, sendPromise);

            expect(useCopilotStore.getState().messages.at(-1)).toEqual({
                content: [
                    {
                        data: {
                            code: 'MODEL_SELECTION_INCOMPLETE',
                            message: 'Your model selection is incomplete. Pick a model again.',
                        },
                        type: 'data-run-error',
                    },
                ],
                id: expect.any(String),
                role: 'assistant',
            });
        });

        it('renders the fallback text when the RUN_ERROR message is empty', async () => {
            renderProvider();

            const agent = hoisted.agents[0];
            const sendPromise = startSend();

            act(() => agent.subscriber?.onRunErrorEvent?.({event: {message: '   '}}));

            await finishRun(agent, sendPromise);

            expect(useCopilotStore.getState().messages.at(-1)).toEqual({
                content: [
                    {data: {message: 'The agent run failed before completing this turn.'}, type: 'data-run-error'},
                ],
                id: expect.any(String),
                role: 'assistant',
            });
        });
    });

    describe('send failures', () => {
        it('shows "Copilot is not ready yet" when there is no conversation', async () => {
            useCopilotStore.setState({conversationId: undefined});

            renderProvider();

            await act(async () => {
                await hoisted.runtimeOptions?.onNew({content: [{text: 'hello', type: 'text'}]});
            });

            expect(hoisted.agents).toHaveLength(0);
            expect(hoisted.toastError).toHaveBeenCalledWith('Copilot is not ready yet');
        });

        it('runs post-turn callbacks when the run rejects', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            const postTurnCallback = vi.fn();

            useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_EDITOR, postTurnCallback);

            renderProvider();

            const agent = hoisted.agents[0];
            const sendPromise = startSend();

            await act(async () => {
                agent.rejectRun?.(new Error('network down'));

                await sendPromise;
            });

            expect(postTurnCallback).toHaveBeenCalledTimes(1);
        });

        it('shows "Failed to send message" when the run rejects', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            renderProvider();

            const agent = hoisted.agents[0];
            const sendPromise = startSend();

            await act(async () => {
                agent.rejectRun?.(new Error('network down'));

                await sendPromise;
            });

            expect(hoisted.toastError).toHaveBeenCalledWith('Failed to send message: network down');
        });

        it('stays silent when the run is aborted because the agent was rebuilt for a new source', async () => {
            renderProvider();

            const agent = hoisted.agents[0];
            const sendPromise = startSend();

            act(() =>
                useCopilotStore.setState({
                    context: {mode: MODE.BUILD, parameters: {}, source: Source.CLUSTER_ELEMENT},
                })
            );

            expect(agent.abortRun).toHaveBeenCalled();

            await act(async () => {
                agent.rejectRun?.(new Error('aborted'));

                await sendPromise;
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('resets isRunning when assembling the run state throws', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            vi.spyOn(useCopilotStateContributorRegistry.getState(), 'contribute').mockImplementation(() => {
                throw new Error('contributor failed');
            });

            renderProvider();

            await act(async () => {
                await hoisted.runtimeOptions?.onNew({content: [{text: 'hello', type: 'text'}]});
            });

            expect(hoisted.toastError).toHaveBeenCalledWith('Failed to send message: contributor failed');
            expect(hoisted.runtimeOptions?.isRunning).toBe(false);
        });
    });

    describe('persisted model', () => {
        const catalogProvider = {
            enabled: true,
            icon: null,
            key: 'ai.provider.openAi',
            models: [{label: 'GPT-4o', name: 'gpt-4o'}],
            name: 'OpenAI',
            supportsModelById: false,
        };

        const sendAndCaptureState = async () => {
            renderProvider();

            const agent = hoisted.agents[0];
            const sendPromise = startSend();

            await finishRun(agent, sendPromise);

            return agent.setState.mock.calls[0][0] as Record<string, unknown>;
        };

        it('sends the persisted model when it is available in an enabled provider', async () => {
            hoisted.providerCatalog = [catalogProvider];

            writeLastUsedModel(7, 'ai.provider.openAi', 'gpt-4o');

            const state = await sendAndCaptureState();

            expect(state).toMatchObject({
                userSelectedLlmModel: 'gpt-4o',
                userSelectedLlmProvider: 'ai.provider.openAi',
            });
            expect(readLastUsedModel(7)).not.toBeNull();
        });

        it('drops and clears the persisted model when its provider is disabled', async () => {
            hoisted.providerCatalog = [{...catalogProvider, enabled: false}];

            writeLastUsedModel(7, 'ai.provider.openAi', 'gpt-4o');

            const state = await sendAndCaptureState();

            expect(state).not.toHaveProperty('userSelectedLlmModel');
            expect(state).not.toHaveProperty('userSelectedLlmProvider');
            expect(readLastUsedModel(7)).toBeNull();
        });

        it('drops and clears the persisted model when the model is no longer offered', async () => {
            hoisted.providerCatalog = [catalogProvider];

            writeLastUsedModel(7, 'ai.provider.openAi', 'gpt-3');

            const state = await sendAndCaptureState();

            expect(state).not.toHaveProperty('userSelectedLlmModel');
            expect(readLastUsedModel(7)).toBeNull();
        });

        it('does not send the persisted model before the catalog has loaded', async () => {
            writeLastUsedModel(7, 'ai.provider.openAi', 'gpt-4o');

            const state = await sendAndCaptureState();

            expect(state).not.toHaveProperty('userSelectedLlmModel');
            expect(readLastUsedModel(7)).not.toBeNull();
        });

        it('sends the model picked in this conversation without consulting the catalog', async () => {
            useCopilotStore.setState({selectedLlmModel: 'claude', selectedLlmProvider: 'ai.provider.anthropic'});

            const state = await sendAndCaptureState();

            expect(state).toMatchObject({
                userSelectedLlmModel: 'claude',
                userSelectedLlmProvider: 'ai.provider.anthropic',
            });
        });
    });
});
