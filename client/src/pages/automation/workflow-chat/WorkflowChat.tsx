import {WorkflowTestChatRuntimeProvider} from '@/pages/automation/workflow-chat/WorkflowChatRuntimeProvider';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {Thread} from '@assistant-ui/react';
import {useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const WorkflowChat = () => {
    const {environment, workflowExecutionId} = useParams();

    const {generateConversationId} = useWorkflowChatStore();

    useEffect(() => {
        generateConversationId();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div className="size-full">
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

                    <div className={twMerge('absolute inset-x-0 bottom-0 text-sm', environment === 'test' && 'top-16')}>
                        <Thread />
                    </div>
                </div>
            </WorkflowTestChatRuntimeProvider>
        </div>
    );
};

export default WorkflowChat;
