import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {JobStatusEnum, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {TabValueType} from '@/shared/types';
import getDeepestFailedExecution from '@/shared/util/getDeepestFailedExecution';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';

const POLLING_INTERVAL_MS = 2000;

const useWorkflowExecutionSheet = () => {
    const [activeTab, setActiveTab] = useState<TabValueType>('output');
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<TaskExecution | TriggerExecution | undefined>(undefined);

    const {setWorkflowExecutionSheetOpen, workflowExecutionId, workflowExecutionSheetOpen} =
        useWorkflowExecutionSheetStore(
            useShallow((state) => ({
                setWorkflowExecutionSheetOpen: state.setWorkflowExecutionSheetOpen,
                workflowExecutionId: state.workflowExecutionId,
                workflowExecutionSheetOpen: state.workflowExecutionSheetOpen,
            }))
        );

    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);

    const copilotEnabled = ai.copilot.enabled;

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

    const jobIdRef = useRef<string | undefined>(undefined);

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

    const handleCopilotClick = useCallback(() => {
        const {
            context: currentContext,
            generateConversationId,
            resetMessages,
            saveConversationState,
        } = useCopilotStore.getState();

        saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [setContext]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState();

        setCopilotPanelOpen(false);
    }, []);

    const handleOpenChange = useCallback(() => {
        if (workflowExecutionSheetOpen) {
            useCopilotStore.getState().restoreConversationState();
            setCopilotPanelOpen(false);
        }

        setWorkflowExecutionSheetOpen(!workflowExecutionSheetOpen);
    }, [workflowExecutionSheetOpen, setWorkflowExecutionSheetOpen]);

    useEffect(() => {
        if (!job?.id || job.id === jobIdRef.current) {
            return;
        }

        jobIdRef.current = job.id;

        const hasNoTaskExecutions = !job.taskExecutions || job.taskExecutions.length === 0;

        const jobFailedWithNoExecutions = hasNoTaskExecutions && job.status === JobStatusEnum.Failed;

        const newActiveTab = jobFailedWithNoExecutions || deepestFailedExecution?.execution.error ? 'error' : 'output';

        setActiveTab(newActiveTab);

        const newSelectedItem =
            deepestFailedExecution?.execution || triggerExecution || job.taskExecutions?.[0] || undefined;

        setSelectedItem(newSelectedItem);
    }, [deepestFailedExecution, job, triggerExecution]);

    return {
        activeTab,
        copilotEnabled,
        copilotPanelOpen,
        deepestFailedExecution,
        dialogOpen,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
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
        workflowExecutionSheetOpen,
    };
};

export default useWorkflowExecutionSheet;
