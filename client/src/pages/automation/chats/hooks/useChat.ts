import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {toEnvironmentName} from '@/shared/constants';
import {useWorkflowChatProjectDeploymentWorkflowQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useMemo} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const useChat = () => {
    const {workflowExecutionId} = useParams();
    const navigate = useNavigate();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const environmentName = useMemo(() => toEnvironmentName(currentEnvironmentId), [currentEnvironmentId]);

    const activeWorkflowExecutionId = useChatsStore((state) => state.activeWorkflowExecutionId);
    const isRunning = useChatsStore((state) => state.isRunning);
    const setCurrentChatName = useChatsStore((state) => state.setCurrentChatName);
    const switchChat = useChatsStore((state) => state.switchChat);

    useEffect(() => {
        if (workflowExecutionId) {
            switchChat(workflowExecutionId);
        }
    }, [workflowExecutionId, switchChat]);

    // If switchChat was blocked (isRunning), redirect back to the active chat
    useEffect(() => {
        if (
            isRunning &&
            activeWorkflowExecutionId &&
            workflowExecutionId &&
            activeWorkflowExecutionId !== workflowExecutionId
        ) {
            navigate(`/automation/chat/${activeWorkflowExecutionId}`, {replace: true});
        }
    }, [activeWorkflowExecutionId, isRunning, navigate, workflowExecutionId]);

    const effectiveWorkflowExecutionId = activeWorkflowExecutionId || workflowExecutionId;

    const {data} = useWorkflowChatProjectDeploymentWorkflowQuery(
        {
            id: effectiveWorkflowExecutionId!,
        },
        {
            enabled: !!effectiveWorkflowExecutionId,
        }
    );

    const chatName = useMemo(() => {
        if (data?.projectDeploymentWorkflow) {
            const workflow = data.projectDeploymentWorkflow.projectWorkflow?.workflow;

            return workflow?.label || 'Untitled Workflow';
        }

        return null;
    }, [data]);

    useEffect(() => {
        setCurrentChatName(chatName);
    }, [chatName, setCurrentChatName]);

    return {
        effectiveWorkflowExecutionId: effectiveWorkflowExecutionId!,
        environmentName,
        sseStreamResponse: data?.projectDeploymentWorkflow?.projectWorkflow?.sseStreamResponse,
    };
};

export default useChat;
