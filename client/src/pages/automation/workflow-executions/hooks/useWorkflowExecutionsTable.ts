import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';

export const useWorkflowExecutionsTable = () => {
    const [expandedJobIds, setExpandedJobIds] = useState<Set<string>>(new Set());

    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore(
        useShallow((state) => ({
            setWorkflowExecutionId: state.setWorkflowExecutionId,
            setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
        }))
    );

    const handleRowClick = (execution: WorkflowExecution) => {
        if (execution.id != null) {
            setWorkflowExecutionId(execution.id);

            setWorkflowExecutionSheetOpen(true);
        }
    };

    const handleToggleExpand = (jobId: string) => {
        setExpandedJobIds((previousExpandedJobIds) => {
            const nextExpandedJobIds = new Set(previousExpandedJobIds);

            if (nextExpandedJobIds.has(jobId)) {
                nextExpandedJobIds.delete(jobId);
            } else {
                nextExpandedJobIds.add(jobId);
            }

            return nextExpandedJobIds;
        });
    };

    return {
        expandedJobIds,
        handleRowClick,
        handleToggleExpand,
    };
};
