import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {WorkflowTestExecutionFromJSON} from '@/shared/middleware/platform/workflow/test/models/WorkflowTestExecution';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

export function useWorkflowTestStream(
    workflowId: string,
    onResult?: (execution: WorkflowTestExecution) => void,
    onError?: () => void,
    onStart?: (jobId: string) => void
) {
    const [streamRequest, setStreamRequest] = useState<SSERequestType>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setWorkflowIsRunning, setWorkflowTestExecution} = useWorkflowEditorStore(
        useShallow((state) => ({
            setWorkflowIsRunning: state.setWorkflowIsRunning,
            setWorkflowTestExecution: state.setWorkflowTestExecution,
        }))
    );

    const {getPersistedJobId, persistJobId} = usePersistJobId(workflowId, currentEnvironmentId);

    const {close, error} = useSSE<WorkflowTestExecution>(streamRequest, {
        eventHandlers: {
            error: () => {
                setWorkflowIsRunning(false);
                setWorkflowTestExecution(undefined);
                setStreamRequest(null);
                persistJobId(null);

                if (onError) {
                    onError();
                }
            },
            result: (data) => {
                try {
                    const workflowTestExecution = WorkflowTestExecutionFromJSON(JSON.parse(data));

                    setWorkflowTestExecution(workflowTestExecution);

                    if (onResult) {
                        onResult(workflowTestExecution);
                    }
                } catch (error) {
                    console.error('Failed to parse workflow test execution result:', error);
                } finally {
                    setWorkflowIsRunning(false);
                    setStreamRequest(null);
                    persistJobId(null);
                }
            },
            start: (data) => {
                const parsed = JSON.parse(data);

                const jobId = String((parsed as {jobId: string | number}).jobId);

                persistJobId(jobId);

                if (onStart) {
                    onStart(jobId);
                }
            },
        },
    });

    return {
        close,
        error,
        getPersistedJobId,
        persistJobId,
        setStreamRequest,
    };
}
