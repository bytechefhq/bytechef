import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {getErrorItem, getInitialSelectedItem} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import {ExecutionError} from '@/shared/middleware/graphql';
import {
    Job,
    JobStatusEnum,
    TaskExecution,
    TriggerExecution,
    WorkflowTestExecution,
} from '@/shared/middleware/platform/workflow/test';
import {TabValueType} from '@/shared/types';
import {useEffect, useState} from 'react';

type UseWorkflowExecutionsReturnType = {
    activeTab: TabValueType;
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

    const hasTaskExecutions = job?.taskExecutions || job?.taskExecutions?.length;

    const jobFailedWithNoExecutions = !hasTaskExecutions && job?.status === JobStatusEnum.Failed;
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

    return {
        activeTab,
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
