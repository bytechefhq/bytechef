import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowChatRuntimeProvider} from '@/pages/automation/workflow-chat/runtime-providers/WorkflowChatRuntimeProvider';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {PRODUCTION_ENVIRONMENT, toEnvironmentName} from '@/shared/constants';
import {useEffect, useMemo} from 'react';
import {useParams, useSearchParams} from 'react-router-dom';

const WorkflowChat = () => {
    const {environmentId, workflowExecutionId} = useParams();
    const [searchParams] = useSearchParams();

    const reset = useWorkflowChatStore((state) => state.reset);

    const environmentName = useMemo(
        () => toEnvironmentName(environmentId ? +environmentId : PRODUCTION_ENVIRONMENT),
        [environmentId]
    );

    const sseStream = useMemo(() => searchParams.get('sseStream') === 'true', [searchParams]);

    useEffect(() => {
        return () => {
            reset();
        };
    }, [reset]);

    return (
        <div className="size-full bg-surface-main">
            <WorkflowChatRuntimeProvider
                environment={environmentName}
                sseStream={sseStream}
                workflowExecutionId={workflowExecutionId ?? ''}
            >
                <div className="flex size-full flex-col">
                    {+(environmentId ?? PRODUCTION_ENVIRONMENT) !== PRODUCTION_ENVIRONMENT && (
                        <div className="absolute space-x-1 p-3 uppercase">
                            <span>Environment:</span>

                            <span className="font-semibold">{environmentName}</span>
                        </div>
                    )}

                    <Thread />
                </div>
            </WorkflowChatRuntimeProvider>
        </div>
    );
};

export default WorkflowChat;
