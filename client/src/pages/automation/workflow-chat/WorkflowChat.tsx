import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowTestChatRuntimeProvider} from '@/pages/automation/workflow-chat/runtime-providers/WorkflowChatRuntimeProvider';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {useEffect} from 'react';
import {useParams} from 'react-router-dom';

const WorkflowChat = () => {
    const {environment, workflowExecutionId} = useParams();

    const {generateConversationId} = useWorkflowChatStore();

    useEffect(() => {
        generateConversationId();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div className="size-full bg-surface-main">
            <WorkflowTestChatRuntimeProvider
                environment={environment ?? 'production'}
                workflowExecutionId={workflowExecutionId ?? ''}
            >
                <div className="flex size-full flex-col">
                    {environment === 'test' && (
                        <div className="space-x-1 p-3 uppercase">
                            <span>Environment:</span>

                            <span className="font-semibold">{environment}</span>
                        </div>
                    )}

                    <Thread />
                </div>
            </WorkflowTestChatRuntimeProvider>
        </div>
    );
};

export default WorkflowChat;
