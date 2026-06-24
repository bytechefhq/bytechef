import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {humanizeAgentErrorMessage} from '@/shared/components/ai-chat/messages/humanizeAgentErrorMessage';
import {parseJson, toToolResultDataPart} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {
    AppendMessage,
    AssistantRuntimeProvider,
    type SuggestionConfig,
    Suggestions,
    ThreadMessageLike,
    useAui,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {ReactNode, useMemo, useState} from 'react';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
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

    const sourceKey = sourceProp ?? context?.source ?? Source.WORKFLOW_EDITOR;

    const agent = useMemo(() => {
        if (!conversationId) {
            return null;
        }

        return new HttpAgent({
            agentId: Source[sourceKey],
            headers: {
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
            },
            threadId: conversationId,
            url: `/api/platform/internal/ai/chat/${Source[sourceKey].toLowerCase()}`,
        });
    }, [conversationId, sourceKey]);

    const runAgentNow = async () => {
        if (!agent) {
            throw new Error('Copilot is not ready: taskId has not been initialized yet');
        }

        setIsRunning(true);

        const contextWithoutError: Record<string, unknown> = {...(context ?? {})};

        delete contextWithoutError.workflowExecutionError;

        const currentWorkspaceId = useWorkspaceStore.getState().currentWorkspaceId;

        const stateToSend = {
            ...contextWithoutError,
            ...useCopilotStateContributorRegistry.getState().contribute(),
            environmentId: String(environmentStore.getState().currentEnvironmentId ?? 0),
            // The connection/property picker tools resolve options against this workspace. userId is NOT
            // sent — the server derives it from the authenticated request (never trust the client for it).
            ...(currentWorkspaceId != null ? {workspaceId: String(currentWorkspaceId)} : {}),
            // Drop half-set picker values client-side rather than sending them. The server
            // tolerates partial input (logs once, falls back to workspace default), but omitting
            // here keeps the wire format clean and reserves the warning log for genuinely-broken state.
            ...(selectedLlmProvider != null && selectedLlmModel != null
                ? {userSelectedLlmModel: selectedLlmModel, userSelectedLlmProvider: selectedLlmProvider}
                : {}),
        };

        agent.setState(stateToSend);

        // Prepare an empty assistant message to stream into
        addMessage({content: '', role: 'assistant'});

        // Tracks the tool name per tool-call id so the result handler can map the result payload to the
        // right shared data-part renderer (the result event carries only the id, not the name).
        const toolCallNamesById = new Map<string, string>();

        const subscriber: AgentSubscriber = {
            onRunErrorEvent: ({event}) => {
                // Surface a whole-run failure inline as a distinct red callout bubble (shared RunErrorMessage),
                // humanized to strip Java FQCNs / unwrap provider JSON envelopes — same treatment AI Hub gives it.
                const rawMessage = typeof event?.message === 'string' ? event.message.trim() : '';
                const message =
                    rawMessage.length > 0
                        ? humanizeAgentErrorMessage(rawMessage)
                        : 'The agent run failed before completing this turn.';

                addMessage({content: [{data: {message}, type: 'data-run-error'}], role: 'assistant'});
            },
            onTextMessageContentEvent: ({event, textMessageBuffer}) => {
                appendToLastAssistantMessage(textMessageBuffer + event.delta);
            },
            onTextMessageEndEvent: ({textMessageBuffer}) => {
                appendToLastAssistantMessage(textMessageBuffer);
            },
            onToolCallResultEvent: ({event}) => {
                const toolCallName = toolCallNamesById.get(event.toolCallId);

                toolCallNamesById.delete(event.toolCallId);

                useCopilotToolResultHandlerRegistry.getState().runFor(toolCallName ?? '', event.content);

                const dataPart = toToolResultDataPart(toolCallName ?? '', event.content);

                if (!dataPart) {
                    return;
                }

                if (!dataPart.ok) {
                    const errorEnvelope = parseJson<{error?: unknown}>(
                        event.content,
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

        try {
            await agent.runAgent({runId: getRandomId()}, subscriber);
        } finally {
            setIsRunning(false);

            useCopilotPostTurnRegistry.getState().runFor(sourceKey);
        }
    };

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        if (!agent) {
            throw new Error('Copilot is not ready: taskId has not been initialized yet');
        }

        const input = message.content[0].text;

        addMessage({content: input, role: 'user'});
        agent.addMessage({content: input, id: getRandomId(), role: 'user'});

        await runAgentNow();
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

        const input = message.content[0].text;

        try {
            // Rewind both layers (UI store + AG-UI agent transcript) to the edit point so the next run sees
            // the corrected history without the stale assistant reply.
            editUserMessage(editedIndex, input);

            agent.setMessages(agent.messages.slice(0, editedIndex));
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

        // parentId is the id of the message immediately before the assistant reply being regenerated. The
        // external-store runtime uses idx-based ids, so it's the index of the preceding user message; null
        // when there is no parent (rare for assistant turns).
        const parentIndex = parentId == null ? -1 : Number(parentId);

        if (!Number.isInteger(parentIndex) || parentIndex < -1) {
            toast.error('Could not resolve message to refresh');

            return;
        }

        try {
            // Truncate the UI store to drop the assistant reply (and anything after) but keep the user turn.
            const truncatedMessages = useCopilotStore.getState().messages.slice(0, parentIndex + 1);

            useCopilotStore.setState({messages: truncatedMessages});

            // Mirror the same truncation on the agent transcript and re-run without re-adding the user turn —
            // it's already in the slice we kept.
            agent.setMessages(agent.messages.slice(0, parentIndex + 1));

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

    const aui = useAui({suggestions: Suggestions(suggestions ?? [])}, {parent: null});

    return (
        <AssistantRuntimeProvider aui={aui} runtime={runtime}>
            {children}
        </AssistantRuntimeProvider>
    );
}
