import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {AppendMessage, AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

export function CopilotRuntimeProvider({
    children,
}: Readonly<{
    children: ReactNode;
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const {addMessage, appendToLastAssistantMessage, context, conversationId, messages} = useCopilotStore(
        useShallow((state) => ({
            addMessage: state.addMessage,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            context: state.context,
            conversationId: state.conversationId,
            messages: state.messages,
        }))
    );
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {projectId, projectWorkflowId} = useParams();

    const agent = new HttpAgent({
        agentId: Source[context.source],
        headers: {
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        threadId: conversationId!,
        url: `/api/platform/internal/ai/sse/${Source[context.source].toLowerCase()}`,
    });

    const queryClient = useQueryClient();

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        addMessage({content: input, role: 'user'});
        setIsRunning(true);

        agent.addMessage({
            content: input,
            id: getRandomId(),
            role: 'user',
        });
        agent.setState({
            ...context,
            workflowId: workflow.id,
        });

        // Prepare an empty assistant message to stream into
        addMessage({content: '', role: 'assistant'});

        const subscriber: AgentSubscriber = {
            onTextMessageContentEvent: ({textMessageBuffer}) => {
                appendToLastAssistantMessage(textMessageBuffer);
            },
        };

        await agent.runAgent(
            {
                runId: getRandomId(),
            },
            subscriber
        );

        setIsRunning(false);

        // if (workflowUpdated) {
        queryClient.invalidateQueries({
            queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
        });
        // }
    };

    const runtime = useExternalStoreRuntime({
        convertMessage,
        isRunning,
        messages,
        onNew,
    });

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
