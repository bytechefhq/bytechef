import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {getErrorItem, getInitialSelectedItem} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {ExecutionError} from '@/shared/middleware/automation/workflow/execution';
import {
    Job,
    JobStatusEnum,
    TaskExecution,
    TriggerExecution,
    WorkflowTestExecution,
} from '@/shared/middleware/platform/workflow/test';
import {TabValueType} from '@/shared/types';
import getDeepestFailedExecution from '@/shared/util/getDeepestFailedExecution';
import {useEffect, useMemo, useRef, useState} from 'react';

type UseWorkflowExecutionsReturnType = {
    activeTab: TabValueType;
    deepestFailedExecution: {execution: TaskExecution | TriggerExecution; path: string[]} | null;
    dialogOpen: boolean;
    handleBreadcrumbNavigate: (index: number) => void;
    handleExecutionClick: (taskExecution: TaskExecution | TriggerExecution) => void;
    handleSeeExecutions: (childJob: Job) => void;
    isTriggerExecution: boolean;
    job?: Job;
    jobFailedWithNoExecutions: boolean;
    jobFailureError: ExecutionError;
    rootJob?: Job;
    selectedExecution?: TaskExecution | TriggerExecution;
    setActiveTab: (activeTab: TabValueType) => void;
    setDialogOpen: (open: boolean) => void;
    subflowStack: Array<{job: Job; label: string}>;
    taskExecutions: TaskExecution[];
    triggerExecution?: TriggerExecution;
};

const RUNNING_JOB_STATUSES: JobStatusEnum[] = [JobStatusEnum.Created, JobStatusEnum.Started];

/**
 * Finds the task execution with the given id anywhere in the tree -- condition/loop children, loop iterations and
 * subflow child jobs included.
 */
function findTaskExecution(taskExecutions: TaskExecution[], id: string): TaskExecution | undefined {
    for (const taskExecution of taskExecutions) {
        if (taskExecution.id === id) {
            return taskExecution;
        }

        const nestedTaskExecutions = [
            ...(taskExecution.children ?? []),
            ...(taskExecution.iterations ?? []).flat(),
            ...(taskExecution.childJob?.taskExecutions ?? []),
        ];

        const nestedTaskExecution = findTaskExecution(nestedTaskExecutions, id);

        if (nestedTaskExecution) {
            return nestedTaskExecution;
        }
    }

    return undefined;
}

/** Finds the subflow child job with the given id anywhere in the tree. */
function findChildJob(taskExecutions: TaskExecution[], jobId: string): Job | undefined {
    for (const taskExecution of taskExecutions) {
        if (taskExecution.childJob?.id === jobId) {
            return taskExecution.childJob;
        }

        const nestedTaskExecutions = [
            ...(taskExecution.children ?? []),
            ...(taskExecution.iterations ?? []).flat(),
            ...(taskExecution.childJob?.taskExecutions ?? []),
        ];

        const childJob = findChildJob(nestedTaskExecutions, jobId);

        if (childJob) {
            return childJob;
        }
    }

    return undefined;
}

const useWorkflowExecutions = ({
    workflowTestExecution,
}: {
    workflowTestExecution?: WorkflowTestExecution;
}): UseWorkflowExecutionsReturnType => {
    const [activeTab, setActiveTab] = useState<TabValueType>('output');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedExecution, setSelectedExecution] = useState<TaskExecution | TriggerExecution | undefined>(
        getInitialSelectedItem(workflowTestExecution)
    );
    const [subflowStack, setSubflowStack] = useState<Array<{job: Job; label: string}>>([]);

    const jobFinishedRef = useRef(false);
    const jobIdRef = useRef<string | undefined>(undefined);

    const {job, triggerExecution} = workflowTestExecution ?? {};

    const rootJobId = job?.id;

    const rootJobFinished = !!job?.status && !RUNNING_JOB_STATUSES.includes(job.status);

    // While a test run is in flight the execution is replaced by a fresh snapshot every few hundred milliseconds, so
    // the subflow stack and the selection are re-resolved by id against the latest one -- holding on to the objects
    // would freeze them at whatever state they had when they were clicked.
    const currentSubflowStack = useMemo(
        () =>
            subflowStack.map((entry) => ({
                ...entry,
                job: (entry.job.id && findChildJob(job?.taskExecutions ?? [], entry.job.id)) || entry.job,
            })),
        [job?.taskExecutions, subflowStack]
    );

    const activeJob = currentSubflowStack.length > 0 ? currentSubflowStack[currentSubflowStack.length - 1].job : job;

    const currentWorkflowId = activeJob?.workflowId;

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
            const result = getDeepestFailedExecution({
                currentPath: [],
                execution: taskExecution,
            });

            if (result) {
                return result;
            }
        }

        return null;
    }, [taskExecutions, triggerExecution]);

    const jobFailedWithNoExecutions = !taskExecutions.length && activeJob?.status === JobStatusEnum.Failed;

    const currentSelectedExecution = useMemo(() => {
        if (!selectedExecution?.id) {
            return selectedExecution;
        }

        if (triggerExecution?.id === selectedExecution.id) {
            return triggerExecution;
        }

        return findTaskExecution(taskExecutions, selectedExecution.id) ?? selectedExecution;
    }, [selectedExecution, taskExecutions, triggerExecution]);

    const jobFailureError = activeJob?.error ?? {
        message: 'Workflow execution failed before any executions were created.',
        stackTrace: [],
    };

    const handleBreadcrumbNavigate = (index: number) => {
        setSubflowStack((prev) => prev.slice(0, index));
    };

    const handleExecutionClick = (taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'output');

        setSelectedExecution(taskExecution);
    };

    const handleSeeExecutions = (childJob: Job) => {
        const label = childJob.label ?? 'Subflow';

        setSubflowStack((prev) => [...prev, {job: childJob, label}]);
    };

    useEffect(() => {
        setSubflowStack([]);
    }, [rootJobId]);

    useEffect(() => {
        const errorItem = getErrorItem(workflowTestExecution);

        if (!errorItem || !activeJob) {
            useCopilotStore.getState().setWorkflowExecutionError(undefined);

            return;
        }

        const {error, title} = errorItem;

        if (error && currentWorkflowId) {
            useCopilotStore.getState().setWorkflowExecutionError({
                errorMessage: error.message,
                stackTrace: error.stackTrace,
                title,
                workflowId: currentWorkflowId,
            });
        } else if (jobFailedWithNoExecutions && activeJob.error && currentWorkflowId) {
            useCopilotStore.getState().setWorkflowExecutionError({
                errorMessage: activeJob.error.message,
                stackTrace: activeJob.error.stackTrace,
                title: 'Workflow',
                workflowId: currentWorkflowId,
            });
        }
    }, [workflowTestExecution, currentWorkflowId, jobFailedWithNoExecutions, activeJob]);

    useEffect(() => {
        if (!activeJob?.id) {
            return;
        }

        const jobChanged = activeJob.id !== jobIdRef.current;
        const jobJustFinished = rootJobFinished && !jobFinishedRef.current;

        jobIdRef.current = activeJob.id;
        jobFinishedRef.current = rootJobFinished;

        const jobFailed = jobFailedWithNoExecutions || !!deepestFailedExecution?.execution.error;

        // Later snapshots of the same run keep whatever the user picked; only a new job, a run that has just finished
        // with a failure to point at, or a first task appearing in a run that had none moves the selection.
        if (!jobChanged && !(jobJustFinished && jobFailed) && selectedExecution) {
            return;
        }

        setActiveTab(jobFailed ? 'error' : 'output');

        setSelectedExecution(
            deepestFailedExecution?.execution || triggerExecution || activeJob.taskExecutions?.[0] || undefined
        );
    }, [
        activeJob,
        deepestFailedExecution,
        jobFailedWithNoExecutions,
        rootJobFinished,
        selectedExecution,
        triggerExecution,
    ]);

    return {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleBreadcrumbNavigate,
        handleExecutionClick,
        handleSeeExecutions,
        isTriggerExecution: currentSelectedExecution?.id === triggerExecution?.id,
        job: activeJob,
        jobFailedWithNoExecutions,
        jobFailureError,
        rootJob: job,
        selectedExecution: currentSelectedExecution,
        setActiveTab,
        setDialogOpen,
        subflowStack: currentSubflowStack,
        taskExecutions,
        triggerExecution,
    };
};

export default useWorkflowExecutions;
