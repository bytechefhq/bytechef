import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowChatRuntimeProvider} from '@/pages/automation/workflow-chat/runtime-providers/WorkflowChatRuntimeProvider';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {toEnvironmentName} from '@/shared/constants';
import {useWorkflowChatProjectDeploymentWorkflowQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useMemo} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const WorkflowChat = () => {
    const {workflowExecutionId} = useParams();
    const navigate = useNavigate();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const environmentName = useMemo(() => toEnvironmentName(currentEnvironmentId), [currentEnvironmentId]);

    const activeWorkflowExecutionId = useWorkflowChatStore((state) => state.activeWorkflowExecutionId);
    const isRunning = useWorkflowChatStore((state) => state.isRunning);
    const setCurrentChatName = useWorkflowChatStore((state) => state.setCurrentChatName);
    const switchChat = useWorkflowChatStore((state) => state.switchChat);

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

    return (
        <div className="flex flex-1">
            <WorkflowChatRuntimeProvider
                environmentName={environmentName}
                sseStreamResponse={data?.projectDeploymentWorkflow?.projectWorkflow?.sseStreamResponse}
                workflowExecutionId={effectiveWorkflowExecutionId!}
            >
                <div className="relative flex size-full flex-col">
                    <Thread />
                </div>
            </WorkflowChatRuntimeProvider>
        </div>
    );
};

export default WorkflowChat;
