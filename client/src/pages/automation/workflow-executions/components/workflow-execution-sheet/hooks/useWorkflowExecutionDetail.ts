import {type LazyTaskExecutionChildrenOptionsI} from '@/shared/components/workflow-executions/LazyTaskExecutionChildren';
import {groupTaskExecutionAttempts} from '@/shared/components/workflow-executions/util/groupTaskExecutionAttempts';
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {
    FocusPathElement,
    Job,
    JobStatusEnum,
    TaskExecution,
    TriggerExecution,
} from '@/shared/middleware/automation/workflow/execution';
import {
    WorkflowExecutionKeys,
    useGetProjectWorkflowExecutionQuery,
    useGetWorkflowExecutionTaskExecutionQuery,
} from '@/shared/queries/automation/workflowExecutions.queries';
import {TabValueType} from '@/shared/types';
import {buildFocusPath} from '@/shared/util/buildFocusPath';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useRef, useState} from 'react';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';

const POLLING_INTERVAL_MS = 2000;

export interface FocusedExecutionI {
    execution: TaskExecution | TriggerExecution;
    path: string[];
}

const useWorkflowExecutionDetail = (workflowExecutionId: number, enabled: boolean) => {
    const [activeTab, setActiveTab] = useState<TabValueType>('output');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [appliedFocus, setAppliedFocus] = useState<{focusPath: FocusPathElement[]; jobId: number} | undefined>(
        undefined
    );
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(undefined);
    const [subflowStack, setSubflowStack] = useState<Array<{jobId: number; label: string}>>([]);

    const workflowRunningRef = useRef(false);

    const workflowExecutionKind = useWorkflowExecutionSheetStore((state) => state.workflowExecutionKind);

    const subflowOpen = subflowStack.length > 0;
    const activeJobId = subflowStack.at(-1)?.jobId ?? workflowExecutionId;
    const activeWorkflowExecutionKind = subflowOpen ? 'JOB' : workflowExecutionKind;

    const queryClient = useQueryClient();

    const {
        data: workflowExecution,
        isError: workflowExecutionError,
        isLoading: workflowExecutionLoading,
        refetch: refetchWorkflowExecution,
    } = useGetProjectWorkflowExecutionQuery(
        {id: activeJobId, lazy: true},
        enabled,
        undefined,
        activeWorkflowExecutionKind
    );

    const {
        data: rootWorkflowExecutionData,
        isError: rootWorkflowExecutionError,
        refetch: refetchRootWorkflowExecution,
    } = useGetProjectWorkflowExecutionQuery(
        {id: workflowExecutionId, lazy: true},
        enabled && subflowOpen,
        undefined,
        workflowExecutionKind
    );

    const isWorkflowRunning = useMemo(() => {
        if (!workflowExecution?.job) {
            return false;
        }

        return getWorkflowStatusType(workflowExecution.job, workflowExecution.triggerExecution) === 'running';
    }, [workflowExecution]);

    useGetProjectWorkflowExecutionQuery(
        {id: activeJobId, lazy: true},
        enabled && isWorkflowRunning,
        POLLING_INTERVAL_MS,
        activeWorkflowExecutionKind
    );

    const rootWorkflowExecution = subflowOpen ? rootWorkflowExecutionData : workflowExecution;

    const job = workflowExecution?.job;
    const rootJob = rootWorkflowExecution?.job;
    const taskExecutionCount = workflowExecution?.taskExecutionCount;
    const triggerExecution = rootWorkflowExecution?.triggerExecution;

    const focusApplied = appliedFocus?.jobId === activeJobId;

    const focusPath = useMemo(
        () => (focusApplied ? appliedFocus!.focusPath : workflowExecution?.focusPath || []),
        [appliedFocus, focusApplied, workflowExecution?.focusPath]
    );

    const triggerExecutionFailed = !!triggerExecution?.error;

    const {
        data: focusedTaskExecution,
        isError: focusedTaskExecutionError,
        refetch: refetchFocusedTaskExecution,
    } = useGetWorkflowExecutionTaskExecutionQuery(
        {id: activeJobId, taskExecutionId: Number(focusPath.at(-1)?.taskExecutionId ?? 0)},
        enabled && !!focusPath.length && !triggerExecutionFailed,
        undefined,
        false
    );

    const taskExecutionAttempts = useMemo(() => groupTaskExecutionAttempts(job?.taskExecutions || []), [job]);

    const focusedTopLevelTaskExecution = useMemo(() => {
        if (!focusedTaskExecutionError || !focusPath.length) {
            return undefined;
        }

        return job?.taskExecutions?.find((taskExecution) => taskExecution.id === focusPath[0].taskExecutionId);
    }, [focusPath, focusedTaskExecutionError, job]);

    const focusedExecution = useMemo<FocusedExecutionI | undefined>(() => {
        if (triggerExecution?.error) {
            return {execution: triggerExecution, path: triggerExecution.id ? [triggerExecution.id] : []};
        }

        const execution = focusedTaskExecution || focusedTopLevelTaskExecution;

        if (!execution || !focusPath.length) {
            return undefined;
        }

        return {execution, path: buildFocusPath(focusPath)};
    }, [focusPath, focusedTaskExecution, focusedTopLevelTaskExecution, triggerExecution]);

    const focusResolved =
        triggerExecutionFailed || !focusPath.length || !!focusedExecution || focusedTaskExecutionError;

    const initialFocusPending = !!job && !focusApplied;

    const jobFailedWithNoExecutions = !job?.taskExecutions?.length && job?.status === JobStatusEnum.Failed;

    const jobFailureError = job?.error ?? {
        message: 'Workflow execution failed before any executions were created.',
        stackTrace: [],
    };

    const isTriggerExecution = selectedItem?.id === triggerExecution?.id;

    const lazy = useMemo<LazyTaskExecutionChildrenOptionsI>(
        () => ({
            focusPath,
            refetchInterval: isWorkflowRunning ? POLLING_INTERVAL_MS : false,
            workflowExecutionId: activeJobId,
        }),
        [activeJobId, focusPath, isWorkflowRunning]
    );

    const handleTaskClick = (taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'output');

        setSelectedItem(taskExecution);
    };

    const handleSeeExecutions = (childJob: Job) => {
        const label = childJob.label ?? 'Subflow';

        setSubflowStack((previousStack) => [...previousStack, {jobId: +childJob.id!, label}]);
    };

    const handleBreadcrumbNavigate = (index: number) => {
        setSubflowStack((previousStack) => previousStack.slice(0, index));
    };

    const handleFocusedTaskExecutionRetry = async () => {
        const {data} = await refetchFocusedTaskExecution();

        if (data) {
            handleTaskClick(data);
        }
    };

    const handleWorkflowExecutionRetry = () => {
        void refetchWorkflowExecution();

        if (subflowOpen) {
            void refetchRootWorkflowExecution();
        }
    };

    useEffect(() => {
        setSubflowStack([]);
    }, [workflowExecutionId]);

    useEffect(() => {
        if (workflowRunningRef.current && !isWorkflowRunning) {
            void queryClient.invalidateQueries({
                queryKey: WorkflowExecutionKeys.workflowExecutionTaskExecutionChildrenOfJob(activeJobId),
            });
        }

        workflowRunningRef.current = isWorkflowRunning;
    }, [activeJobId, isWorkflowRunning, queryClient]);

    useEffect(() => {
        if (!rootJob && triggerExecution && selectedItem?.id !== triggerExecution.id) {
            setActiveTab(triggerExecution.error ? 'error' : 'input');

            setSelectedItem(triggerExecution);
        }
    }, [rootJob, selectedItem?.id, triggerExecution]);

    useEffect(() => {
        if (!job?.id || focusApplied || !focusResolved) {
            return;
        }

        setAppliedFocus({focusPath, jobId: activeJobId});

        const hasNoTaskExecutions = !job.taskExecutions || job.taskExecutions.length === 0;
        const jobFailed = hasNoTaskExecutions && job.status === JobStatusEnum.Failed;
        const newActiveTab = jobFailed || focusedExecution?.execution.error ? 'error' : 'output';

        setActiveTab(newActiveTab);

        setSelectedItem(focusedExecution?.execution || triggerExecution || job.taskExecutions?.[0] || undefined);
    }, [activeJobId, focusApplied, focusPath, focusResolved, focusedExecution, job, triggerExecution]);

    return {
        activeTab,
        dialogOpen,
        focusedExecution,
        focusedTaskExecutionError: focusedTaskExecutionError && !focusedTaskExecution && !triggerExecutionFailed,
        handleBreadcrumbNavigate,
        handleFocusedTaskExecutionRetry,
        handleSeeExecutions,
        handleTaskClick,
        handleWorkflowExecutionRetry,
        isTriggerExecution,
        job,
        jobFailedWithNoExecutions,
        jobFailureError,
        lazy,
        rootJob,
        selectedItem,
        setActiveTab,
        setDialogOpen,
        subflowStack,
        taskExecutionAttempts,
        taskExecutionCount,
        triggerExecution,
        workflowExecution: rootWorkflowExecution,
        workflowExecutionError:
            (workflowExecutionError && !workflowExecution) ||
            (subflowOpen && rootWorkflowExecutionError && !rootWorkflowExecutionData),
        workflowExecutionLoading: workflowExecutionLoading || initialFocusPending,
        workflowExecutionRefetchError: workflowExecutionError && !!workflowExecution,
    };
};

export default useWorkflowExecutionDetail;
