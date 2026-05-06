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
    handleExecutionClick: (taskExecution: TaskExecution | TriggerExecution) => void;
    isTriggerExecution: boolean;
    job?: Job;
    jobFailedWithNoExecutions: boolean;
    jobFailureError: ExecutionError;
    selectedExecution?: TaskExecution | TriggerExecution;
    setActiveTab: (activeTab: TabValueType) => void;
    setDialogOpen: (open: boolean) => void;
    taskExecutions: TaskExecution[];
    triggerExecution?: TriggerExecution;
};

const useWorkflowExecutions = ({
    workflowTestExecution,
}: {
    workflowTestExecution: WorkflowTestExecution;
}): UseWorkflowExecutionsReturnType => {
    const [activeTab, setActiveTab] = useState<TabValueType>('output');
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedExecution, setSelectedExecution] = useState<TaskExecution | TriggerExecution | undefined>(
        getInitialSelectedItem(workflowTestExecution)
    );

    const {job, triggerExecution} = workflowTestExecution;

    const currentWorkflowId = job?.workflowId;

    const jobIdRef = useRef<string | undefined>(undefined);

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

    const jobFailedWithNoExecutions = !taskExecutions.length && job?.status === JobStatusEnum.Failed;
    const jobFailureError = job?.error ?? {
        message: 'Workflow execution failed before any executions were created.',
        stackTrace: [],
    };

    const handleExecutionClick = (taskExecution: TaskExecution | TriggerExecution) => {
        setActiveTab(taskExecution.error ? 'error' : 'output');

        setSelectedExecution(taskExecution);
    };

    useEffect(() => {
        setSelectedExecution(getInitialSelectedItem(workflowTestExecution));

        setActiveTab('output');
    }, [workflowTestExecution]);

    useEffect(() => {
        const errorItem = getErrorItem(workflowTestExecution);

        if (!errorItem || !job) {
            useCopilotStore.getState().setWorkflowExecutionError(undefined);

            return;
        }

        const {error, title} = errorItem;

        if (error && currentWorkflowId) {
            useCopilotStore.getState().setWorkflowExecutionError({
                errorMessage: error.message,
                stackTrace: error.stackTrace,
                title: title,
                workflowId: currentWorkflowId,
            });
        } else if (jobFailedWithNoExecutions && job.error && currentWorkflowId) {
            useCopilotStore.getState().setWorkflowExecutionError({
                errorMessage: job.error.message,
                stackTrace: job.error.stackTrace,
                title: 'Workflow',
                workflowId: currentWorkflowId,
            });
        }
    }, [workflowTestExecution, currentWorkflowId, jobFailedWithNoExecutions, job]);

    useEffect(() => {
        if (!job?.id || job.id === jobIdRef.current) {
            return;
        }

        jobIdRef.current = job.id;

        const hasNoTaskExecutions = !job.taskExecutions || job.taskExecutions.length === 0;

        const jobFailedWithNoExecutions = hasNoTaskExecutions && job.status === JobStatusEnum.Failed;

        const newActiveTab = jobFailedWithNoExecutions || deepestFailedExecution?.execution.error ? 'error' : 'output';

        setActiveTab(newActiveTab);

        const newSelectedExecution =
            deepestFailedExecution?.execution || triggerExecution || job.taskExecutions?.[0] || undefined;

        setSelectedExecution(newSelectedExecution);
    }, [deepestFailedExecution, job, triggerExecution]);

    return {
        activeTab,
        deepestFailedExecution,
        dialogOpen,
        handleExecutionClick,
        isTriggerExecution: selectedExecution?.id === triggerExecution?.id,
        job,
        jobFailedWithNoExecutions,
        jobFailureError,
        selectedExecution,
        setActiveTab,
        setDialogOpen,
        taskExecutions: job?.taskExecutions || [],
        triggerExecution,
    };
};

export default useWorkflowExecutions;
