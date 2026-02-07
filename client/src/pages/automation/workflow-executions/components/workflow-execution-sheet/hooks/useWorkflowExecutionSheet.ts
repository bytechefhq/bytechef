import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useCallback, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';

const POLLING_INTERVAL_MS = 2000;

const useWorkflowExecutionSheet = () => {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);

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

    return {
        copilotEnabled,
        copilotPanelOpen,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        workflowExecution,
        workflowExecutionLoading,
        workflowExecutionSheetOpen,
    };
};

export default useWorkflowExecutionSheet;
