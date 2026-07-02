import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {Job, JobStatusEnum, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {TabValueType} from '@/shared/types';
import getDeepestFailedExecution from '@/shared/util/getDeepestFailedExecution';
import {useEffect, useMemo, useRef, useState} from 'react';

const POLLING_INTERVAL_MS = 2000;

const useWorkflowExecutionDetail = (workflowExecutionId: number, enabled: boolean) => {
    const [activeTab, setActiveTab] = useState<TabValueType>('output');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(undefined);
    const [subflowStack, setSubflowStack] = useState<Array<{job: Job; label: string}>>([]);

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

    const rootJob = workflowExecution?.job;
    const triggerExecution = workflowExecution?.triggerExecution;

    const activeJob = subflowStack.length > 0 ? subflowStack[subflowStack.length - 1].job : rootJob;

    const taskExecutions = useMemo(() => activeJob?.taskExecutions || [], [activeJob?.taskExecutions]);

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

    const jobFailedWithNoExecutions = !activeJob?.taskExecutions?.length && activeJob?.status === JobStatusEnum.Failed;

    const jobFailureError = activeJob?.error ?? {
        message: 'Workflow execution failed before any executions were created.',
        stackTrace: [],
    };

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    const handleTaskClick = (taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'output');

        setSelectedItem(taskExecution);
    };

    const handleSeeExecutions = (childJob: Job) => {
        const label = childJob.label ?? 'Subflow';

        setSubflowStack((previousStack) => [...previousStack, {job: childJob, label}]);
    };

    const handleBreadcrumbNavigate = (index: number) => {
        setSubflowStack((previousStack) => previousStack.slice(0, index));
    };

    useEffect(() => {
        setSubflowStack([]);
    }, [workflowExecutionId]);

    useEffect(() => {
        if (!activeJob?.id || activeJob.id === jobIdRef.current) {
            return;
        }

        jobIdRef.current = activeJob.id;

        const hasNoTaskExecutions = !activeJob.taskExecutions || activeJob.taskExecutions.length === 0;
        const jobFailed = hasNoTaskExecutions && activeJob.status === JobStatusEnum.Failed;
        const newActiveTab = jobFailed || deepestFailedExecution?.execution.error ? 'error' : 'output';

        setActiveTab(newActiveTab);

        setSelectedItem(
            deepestFailedExecution?.execution || triggerExecution || activeJob.taskExecutions?.[0] || undefined
        );
    }, [deepestFailedExecution, activeJob, triggerExecution]);

    return {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleBreadcrumbNavigate,
        handleSeeExecutions,
        handleTaskClick,
        isTriggerExecution,
        job: activeJob,
        jobFailedWithNoExecutions,
        jobFailureError,
        rootJob,
        selectedItem,
        setActiveTab,
        setDialogOpen,
        subflowStack,
        taskExecutions,
        triggerExecution,
        workflowExecution,
        workflowExecutionLoading,
    };
};

export default useWorkflowExecutionDetail;
