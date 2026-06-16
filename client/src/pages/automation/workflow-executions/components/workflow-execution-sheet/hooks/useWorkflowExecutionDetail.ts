import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {JobStatusEnum, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {TabValueType} from '@/shared/types';
import getDeepestFailedExecution from '@/shared/util/getDeepestFailedExecution';
import {useEffect, useMemo, useRef, useState} from 'react';

const POLLING_INTERVAL_MS = 2000;

const useWorkflowExecutionDetail = (workflowExecutionId: number, enabled: boolean) => {
    const [activeTab, setActiveTab] = useState<TabValueType>('output');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(undefined);

    const jobIdRef = useRef<string | undefined>(undefined);

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetProjectWorkflowExecutionQuery(
        {id: workflowExecutionId},
        enabled
    );

    const isWorkflowRunning = useMemo(() => {
        if (!workflowExecution?.job) {
            return false;
        }

        return getWorkflowStatusType(workflowExecution.job, workflowExecution.triggerExecution) === 'running';
    }, [workflowExecution]);

    useGetProjectWorkflowExecutionQuery({id: workflowExecutionId}, enabled && isWorkflowRunning, POLLING_INTERVAL_MS);

    const job = workflowExecution?.job;
    const triggerExecution = workflowExecution?.triggerExecution;

    const taskExecutions = useMemo(() => job?.taskExecutions || [], [job?.taskExecutions]);

    const deepestFailedExecution = useMemo(() => {
        if (triggerExecution) {
            const result = getDeepestFailedExecution({
                currentPath: [],
                execution: triggerExecution,
                isTriggerExecution: true,
            });

            if (result) {
                return result;
            }
        }

        for (const taskExecution of taskExecutions) {
            const result = getDeepestFailedExecution({currentPath: [], execution: taskExecution});

            if (result) {
                return result;
            }
        }

        return null;
    }, [taskExecutions, triggerExecution]);

    const jobFailedWithNoExecutions = !job?.taskExecutions?.length && job?.status === JobStatusEnum.Failed;

    const jobFailureError = job?.error ?? {
        message: 'Workflow execution failed before any executions were created.',
        stackTrace: [],
    };

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    const handleTaskClick = (taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'output');

        setSelectedItem(taskExecution);
    };

    useEffect(() => {
        if (!job?.id || job.id === jobIdRef.current) {
            return;
        }

        jobIdRef.current = job.id;

        const hasNoTaskExecutions = !job.taskExecutions || job.taskExecutions.length === 0;
        const jobFailed = hasNoTaskExecutions && job.status === JobStatusEnum.Failed;
        const newActiveTab = jobFailed || deepestFailedExecution?.execution.error ? 'error' : 'output';

        setActiveTab(newActiveTab);

        setSelectedItem(deepestFailedExecution?.execution || triggerExecution || job.taskExecutions?.[0] || undefined);
    }, [deepestFailedExecution, job, triggerExecution]);

    return {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleTaskClick,
        isTriggerExecution,
        jobFailedWithNoExecutions,
        jobFailureError,
        selectedItem,
        setActiveTab,
        setDialogOpen,
        taskExecutions,
        workflowExecution,
        workflowExecutionLoading,
    };
};

export default useWorkflowExecutionDetail;
