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

    const {job, triggerExecution} = workflowTestExecution ?? {};

    const activeJob = subflowStack.length > 0 ? subflowStack[subflowStack.length - 1].job : job;

    const currentWorkflowId = activeJob?.workflowId;

    const jobIdRef = useRef<string | undefined>(undefined);

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
        setSelectedExecution(getInitialSelectedItem(workflowTestExecution));

        setActiveTab('output');
        setSubflowStack([]);
    }, [workflowTestExecution]);

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
        if (!activeJob?.id || activeJob.id === jobIdRef.current) {
            return;
        }

        jobIdRef.current = activeJob.id;

        const hasNoTaskExecutions = !activeJob.taskExecutions || activeJob.taskExecutions.length === 0;

        const jobFailedWithNoExecutions = hasNoTaskExecutions && activeJob.status === JobStatusEnum.Failed;

        const newActiveTab = jobFailedWithNoExecutions || deepestFailedExecution?.execution.error ? 'error' : 'output';

        setActiveTab(newActiveTab);

        const newSelectedExecution =
            deepestFailedExecution?.execution || triggerExecution || activeJob.taskExecutions?.[0] || undefined;

        setSelectedExecution(newSelectedExecution);
    }, [deepestFailedExecution, activeJob, triggerExecution]);

    return {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleBreadcrumbNavigate,
        handleExecutionClick,
        handleSeeExecutions,
        isTriggerExecution: selectedExecution?.id === triggerExecution?.id,
        job: activeJob,
        jobFailedWithNoExecutions,
        jobFailureError,
        rootJob: job,
        selectedExecution,
        setActiveTab,
        setDialogOpen,
        subflowStack,
        taskExecutions,
        triggerExecution,
    };
};

export default useWorkflowExecutions;
