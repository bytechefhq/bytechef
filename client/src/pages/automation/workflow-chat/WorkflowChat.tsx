import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowChatRuntimeProvider} from '@/pages/automation/workflow-chat/runtime-providers/WorkflowChatRuntimeProvider';
import {toEnvironmentName} from '@/shared/constants';
import {useWorkflowChatProjectDeploymentWorkflowQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';
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

    const chatName = useMemo(() => {
        if (data?.projectDeploymentWorkflow) {
            const workflow = data.projectDeploymentWorkflow.projectWorkflow?.workflow;

            return workflow?.label || 'Untitled Workflow';
        }

        return null;
    }, [data]);

    return (
        <div className="flex flex-1">
            <WorkflowChatRuntimeProvider
                environmentName={environmentName}
                sseStreamResponse={data?.projectDeploymentWorkflow?.projectWorkflow?.sseStreamResponse}
                workflowExecutionId={workflowExecutionId!}
            >
                <div className="relative flex size-full flex-col pt-14">
                    <div className="absolute top-0 flex items-center space-x-4 p-4">
                        {chatName && (
                            <div className="space-x-1">
                                <span className="font-semibold">{chatName}</span>
                            </div>
                        )}
                    </div>

                    <Thread />
                </div>
            </WorkflowChatRuntimeProvider>
        </div>
    );
};

export default WorkflowChat;
