import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowTestChatRuntimeProvider} from '@/pages/automation/workflow-chat/runtime-providers/WorkflowChatRuntimeProvider';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
import {useEffect} from 'react';
import {useParams} from 'react-router-dom';

const WorkflowChat = () => {
    const {environmentId, workflowExecutionId} = useParams();

    const generateConversationId = useWorkflowChatStore((state) => state.generateConversationId);

    const environmentName = toEnvironmentName(environmentId ? +environmentId : PRODUCTION_ENVIRONMENT);

    useEffect(() => {
        generateConversationId();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div className="size-full bg-surface-main">
            <WorkflowTestChatRuntimeProvider
                environment={environmentName}
                workflowExecutionId={workflowExecutionId ?? ''}
            >
                <div className="flex size-full flex-col">
                    {+(environmentId ?? PRODUCTION_ENVIRONMENT) !== PRODUCTION_ENVIRONMENT && (
                        <div className="space-x-1 p-3 uppercase">
                            <span>Environment:</span>

                            <span className="font-semibold">{environmentName}</span>
                        </div>
                    )}

                    <Thread />
                </div>
            </WorkflowTestChatRuntimeProvider>
        </div>
    );
};

export default WorkflowChat;
