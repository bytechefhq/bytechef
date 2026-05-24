import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {AppendMessage, AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

export function CopilotRuntimeProvider({
    children,
    source: sourceProp,
}: Readonly<{
    children: ReactNode;
    source?: Source;
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const {addMessage, appendToLastAssistantMessage, context, conversationId, editUserMessage, messages} =
        useCopilotStore(
            useShallow((state) => ({
                addMessage: state.addMessage,
                appendToLastAssistantMessage: state.appendToLastAssistantMessage,
                context: state.context,
                conversationId: state.conversationId,
                editUserMessage: state.editUserMessage,
                messages: state.messages,
            }))
        );
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const {projectId, projectWorkflowId} = useParams();

    const sourceKey = sourceProp ?? context?.source ?? Source.WORKFLOW_EDITOR;

    // Memoize the agent so it isn't reconstructed on every render. Without this, every render dropped the
    // accumulated agent.addMessage history and any in-flight onNew closure captured a stale reference.
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

        const stateToSend = {
            ...contextWithoutError,
            workspaceId: currentWorkspaceId,
            ...useCopilotStateContributorRegistry.getState()
                .contribute(),
        };

        agent.setState(stateToSend);

        // Prepare an empty assistant message to stream into
        addMessage({content: '', role: 'assistant'});

        const subscriber: AgentSubscriber = {
            onTextMessageContentEvent: ({event, textMessageBuffer}) => {
                appendToLastAssistantMessage(textMessageBuffer + event.delta);
            },
            onTextMessageEndEvent: ({textMessageBuffer}) => {
                appendToLastAssistantMessage(textMessageBuffer);
            },
        };

        try {
            await agent.runAgent({runId: getRandomId()}, subscriber);
        } finally {
            setIsRunning(false);

            // Fire whatever post-turn callback the active consumer has registered for this Source —
            // keeps source-specific refresh logic out of the shared provider.
            useCopilotPostTurnRegistry.getState().runFor(sourceKey);

            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
            });
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

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
