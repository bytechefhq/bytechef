import {useGetIntegrationWorkflowExecutionQuery} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useCallback, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';

const POLLING_INTERVAL_MS = 2000;

const useWorkflowExecutionSheet = () => {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);

    const conversationTokenRef = useRef<string | null>(null);

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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const copilotEnabled = ai.copilot.enabled;

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetIntegrationWorkflowExecutionQuery(
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

    useGetIntegrationWorkflowExecutionQuery(
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

        conversationTokenRef.current = saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {
                environmentId: workflowExecution?.integrationInstanceConfiguration?.environmentId,
                workflowExecutionId,
                workflowId: workflowExecution?.job?.workflowId,
                workspaceId: currentWorkspaceId,
            },
            source: Source.WORKFLOW_EXECUTION_EMBEDDED,
        });

        setCopilotPanelOpen(true);
    }, [currentWorkspaceId, setContext, workflowExecution, workflowExecutionId]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);

        setCopilotPanelOpen(false);
    }, []);

    const handleOpenChange = useCallback(() => {
        if (workflowExecutionSheetOpen) {
            useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);
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
        workflowExecutionId,
        workflowExecutionLoading,
        workflowExecutionSheetOpen,
    };
};

export default useWorkflowExecutionSheet;
