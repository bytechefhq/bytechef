import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useCallback, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';

const POLLING_INTERVAL_MS = 2000;

const useWorkflowExecutionSheet = () => {
    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore(
            useShallow((state) => ({
                setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
                workflowExecutionId: state.workflowExecutionId,
                workflowExecutionSheetOpen: state.workflowExecutionSheetOpen,
            }))
        );

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetProjectWorkflowExecutionQuery(
        {
            id: workflowExecutionId,
        },
        workflowExecutionSheetOpen
    );

    const isWorkflowRunning = useMemo(() => {
        if (!workflowExecution?.job) {
            return false;
        }

        return getWorkflowStatusType(workflowExecution.job, workflowExecution.triggerExecution) === 'running';
    }, [workflowExecution]);

    useGetProjectWorkflowExecutionQuery(
        {id: workflowExecutionId},
        workflowExecutionSheetOpen && isWorkflowRunning,
        POLLING_INTERVAL_MS
    );

    const handleOpenChange = useCallback(() => {
        setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen);
    }, [workflowExecutionSheetOpen, setWorkflowExecutionSheetOpen]);

    return {
        handleOpenChange,
        workflowExecution,
        workflowExecutionLoading,
        workflowExecutionSheetOpen,
    };
};

export default useWorkflowExecutionSheet;
