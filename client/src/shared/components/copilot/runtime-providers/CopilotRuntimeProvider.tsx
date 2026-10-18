import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {toRunErrorData} from '@/shared/components/ai-chat/messages/toRunErrorData';
import {parseJson, toToolResultDataPart} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {findTurnRewindIndexes} from '@/shared/components/copilot/utils/findTurnRewindIndexes';
import {AiProviderCatalogQuery, useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {environmentStore, useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent, contentToText} from '@ag-ui/client';
import {
    AppendMessage,
    AssistantRuntimeProvider,
    AuiConfig,
    type SuggestionConfig,
    Suggestions,
    ThreadMessageLike,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {ReactNode, useEffect, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

const isAvailableModel = (
    providerCatalog: AiProviderCatalogQuery['aiProviderCatalog'],
    providerKey: string,
    modelName: string
): boolean => {
    const provider = providerCatalog.find((candidate) => candidate?.key === providerKey);

    if (provider == null || !provider.enabled) {
        return false;
    }

    return provider.supportsModelById || provider.models.some((model) => model.name === modelName);
};

const resolvePersistedModel = (
    workspaceId: number | null | undefined,
    providerCatalog: AiProviderCatalogQuery['aiProviderCatalog'] | undefined
): {model: string; provider: string} | null => {
    if (workspaceId == null || providerCatalog == null) {
        return null;
    }

    const lastUsedModel = readLastUsedModel(workspaceId);

    if (lastUsedModel == null) {
        return null;
    }

    if (!isAvailableModel(providerCatalog, lastUsedModel.provider, lastUsedModel.model)) {
        writeLastUsedModel(workspaceId, null, null);

        return null;
    }

    return lastUsedModel;
};

export function CopilotRuntimeProvider({
    children,
    source: sourceProp,
    suggestions,
}: Readonly<{
    children: ReactNode;
    source?: Source;
    suggestions?: SuggestionConfig[];
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const abortedAgentsRef = useRef(new WeakSet<HttpAgent>());

    const {
        addMessage,
        appendToLastAssistantMessage,
        context,
        conversationId,
        editUserMessage,
        messages,
        selectedLlmModel,
        selectedLlmProvider,
    } = useCopilotStore(
        useShallow((state) => ({
            addMessage: state.addMessage,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            context: state.context,
            conversationId: state.conversationId,
            editUserMessage: state.editUserMessage,
            messages: state.messages,
            selectedLlmModel: state.selectedLlmModel,
            selectedLlmProvider: state.selectedLlmProvider,
        }))
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const catalogEnvironment = context?.environmentId ?? currentEnvironmentId;

    const {data: providerCatalogData} = useAiProviderCatalogQuery(
        {environment: String(catalogEnvironment)},
        {enabled: catalogEnvironment >= 0}
    );

    const sourceKey = sourceProp ?? context?.source ?? Source.WORKFLOW_EDITOR;

    const agent = useMemo(() => {
        if (!conversationId) {
            return null;
        }

        return new HttpAgent({
            agentId: sourceKey,
            headers: {
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
            },
            threadId: conversationId,
            url: `/api/platform/internal/ai/chat/${sourceKey.toLowerCase()}`,
        });
    }, [conversationId, sourceKey]);

    const runAgentNow = async () => {
        if (!agent) {
            throw new Error('Copilot is not ready: taskId has not been initialized yet');
        }

        const runConversationId = conversationId;

        const isStaleRun = () =>
            useCopilotStore.getState().conversationId !== runConversationId || abortedAgentsRef.current.has(agent);

        const toolCallNamesById = new Map<string, string>();

        let runFailed = false;
        let textMessageHasBubble = false;

        const subscriber: AgentSubscriber = {
            onRunErrorEvent: ({event}) => {
                if (isStaleRun()) {
                    return;
                }

                runFailed = true;

                addMessage({
                    content: [
                        {data: toRunErrorData({code: event?.code, message: event?.message}), type: 'data-run-error'},
                    ],
                    role: 'assistant',
                });
            },
            onTextMessageContentEvent: ({event, textMessageBuffer}) => {
                if (isStaleRun()) {
                    return;
                }

                const text = textMessageBuffer + event.delta;

                if (textMessageHasBubble) {
                    appendToLastAssistantMessage(text);
                } else {
                    textMessageHasBubble = true;

                    addMessage({content: text, role: 'assistant'});
                }
            },
            onTextMessageEndEvent: ({textMessageBuffer}) => {
                if (isStaleRun()) {
                    return;
                }

                if (textMessageHasBubble) {
                    appendToLastAssistantMessage(textMessageBuffer);
                }
            },
            onTextMessageStartEvent: () => {
                textMessageHasBubble = false;
            },
            onToolCallResultEvent: ({event}) => {
                if (isStaleRun()) {
                    return;
                }

                const toolCallName = toolCallNamesById.get(event.toolCallId);

                toolCallNamesById.delete(event.toolCallId);

                const toolResultContent = contentToText(event.content);

                useCopilotToolResultHandlerRegistry.getState().runFor(toolCallName ?? '', toolResultContent);

                const dataPart = toToolResultDataPart(toolCallName ?? '', toolResultContent);

                if (!dataPart) {
                    return;
                }

                if (!dataPart.ok) {
                    const errorEnvelope = parseJson<{error?: unknown}>(
                        toolResultContent,
                        'copilot tool-result error envelope'
                    );
                    const envelopeError =
                        errorEnvelope != null && typeof errorEnvelope.error === 'string' ? errorEnvelope.error : null;

                    aiChatToolCallStore
                        .getState()
                        .completeToolCall(event.toolCallId, {error: envelopeError ?? dataPart.errorMessage}, true);

                    if (envelopeError == null) {
                        toast.error(dataPart.errorMessage);
                    }

                    return;
                }

                addMessage({
                    content: [{data: dataPart.data, type: dataPart.type as `data-${string}`}],
                    role: 'assistant',
                });
            },
            onToolCallStartEvent: ({event}) => {
                toolCallNamesById.set(event.toolCallId, event.toolCallName);
            },
        };

        setIsRunning(true);

        try {
            const contextWithoutError: Record<string, unknown> = {...(context ?? {})};

            delete contextWithoutError.workflowExecutionError;

            const currentWorkspaceId = useWorkspaceStore.getState().currentWorkspaceId;

            const persistedModel =
                selectedLlmProvider != null && selectedLlmModel != null
                    ? null
                    : resolvePersistedModel(currentWorkspaceId, providerCatalogData?.aiProviderCatalog);

            const userSelectedLlmProvider = selectedLlmProvider ?? persistedModel?.provider ?? null;
            const userSelectedLlmModel = selectedLlmModel ?? persistedModel?.model ?? null;

            agent.setState({
                ...contextWithoutError,
                ...useCopilotStateContributorRegistry.getState().contribute(),
                environmentId: String(context?.environmentId ?? environmentStore.getState().currentEnvironmentId ?? 0),
                ...(currentWorkspaceId != null ? {workspaceId: String(currentWorkspaceId)} : {}),
                ...(userSelectedLlmProvider != null && userSelectedLlmModel != null
                    ? {userSelectedLlmModel, userSelectedLlmProvider}
                    : {}),
            });

            await agent.runAgent({runId: getRandomId()}, subscriber);

            if (!runFailed && !isStaleRun()) {
                useCopilotPostTurnRegistry.getState().runFor(sourceKey);
            }
        } catch (error) {
            if (!isStaleRun()) {
                throw error;
            }
        } finally {
            setIsRunning(false);
        }
    };

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            toast.error('Only text messages are supported');

            return;
        }

        if (!agent) {
            toast.error('Copilot is not ready yet');

            return;
        }

        const input = message.content[0].text;

        try {
            addMessage({content: input, role: 'user'});
            agent.addMessage({content: input, id: getRandomId(), role: 'user'});

            await runAgentNow();
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            console.error('Copilot send failed:', error);
            toast.error(`Failed to send message: ${errorMessage}`);
        }
    };

    const onEdit = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            toast.error('Only text messages are supported');

            return;
        }

        if (!agent) {
            toast.error('Copilot is not ready yet');

            return;
        }

        // The external-store runtime assigns each message id = String(index), so sourceId is the array index
        // of the user message being edited.
        const editedIndex = Number(message.sourceId);

        if (!Number.isInteger(editedIndex) || editedIndex < 0) {
            toast.error('Could not resolve edited message');

            return;
        }

        const turnRewindIndexes = findTurnRewindIndexes(
            useCopilotStore.getState().messages,
            agent.messages,
            editedIndex
        );

        if (!turnRewindIndexes || turnRewindIndexes.userMessageIndex !== editedIndex) {
            toast.error('Could not resolve edited message');

            return;
        }

        const input = message.content[0].text;

        try {
            // Rewind both layers (UI store + AG-UI agent transcript) to the edit point so the next run sees
            // the corrected history without the stale assistant reply.
            editUserMessage(editedIndex, input);

            agent.setMessages(agent.messages.slice(0, turnRewindIndexes.agentUserMessageIndex));
            agent.addMessage({content: input, id: getRandomId(), role: 'user'});

            await runAgentNow();
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            console.error('Copilot edit failed:', error);
            toast.error(`Failed to update message: ${errorMessage}`);
        }
    };

    const onReload = async (parentId: string | null) => {
        if (!agent) {
            toast.error('Copilot is not ready yet');

            return;
        }

        const parentIndex = parentId == null ? -1 : Number(parentId);

        const turnRewindIndexes = Number.isInteger(parentIndex)
            ? findTurnRewindIndexes(useCopilotStore.getState().messages, agent.messages, parentIndex)
            : null;

        if (!turnRewindIndexes) {
            toast.error('Could not resolve message to refresh');

            return;
        }

        try {
            const truncatedMessages = useCopilotStore
                .getState()
                .messages.slice(0, turnRewindIndexes.userMessageIndex + 1);

            useCopilotStore.setState({messages: truncatedMessages});

            agent.setMessages(agent.messages.slice(0, turnRewindIndexes.agentUserMessageIndex + 1));

            await runAgentNow();
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            toast.error(`Failed to refresh response: ${errorMessage}`);
        }
    };

    const runtime = useExternalStoreRuntime({
        convertMessage,
        isRunning,
        messages,
        onEdit,
        onNew,
        onReload,
    });

    useEffect(() => {
        const abortedAgents = abortedAgentsRef.current;

        if (agent) {
            abortedAgents.delete(agent);
        }

        return () => {
            if (agent) {
                abortedAgents.add(agent);

                agent.abortRun();
            }
        };
    }, [agent]);

    return (
        <AssistantRuntimeProvider config={AuiConfig({suggestions: Suggestions(suggestions ?? [])})} runtime={runtime}>
            {children}
        </AssistantRuntimeProvider>
    );
}
