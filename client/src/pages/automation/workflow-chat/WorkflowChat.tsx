import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowChatRuntimeProvider} from '@/pages/automation/workflow-chat/runtime-providers/WorkflowChatRuntimeProvider';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {toEnvironmentName} from '@/shared/constants';
import {useWorkflowChatProjectDeploymentWorkflowQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useMemo} from 'react';
import {useParams} from 'react-router-dom';

const WorkflowChat = () => {
    const {workflowExecutionId} = useParams();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const environmentName = useMemo(() => toEnvironmentName(currentEnvironmentId), [currentEnvironmentId]);

    const {data} = useWorkflowChatProjectDeploymentWorkflowQuery(
        {
            id: workflowExecutionId!,
        },
        {
            enabled: !!workflowExecutionId,
        }
    );

    const setCurrentChatName = useWorkflowChatStore((state) => state.setCurrentChatName);

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
                workflowExecutionId={workflowExecutionId!}
            >
                <div className="relative flex size-full flex-col">
                    <Thread />
                </div>
            </WorkflowChatRuntimeProvider>
        </div>
    );
};

export default WorkflowChat;
