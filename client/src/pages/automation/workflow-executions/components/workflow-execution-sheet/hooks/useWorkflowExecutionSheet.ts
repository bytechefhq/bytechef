import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useGetProjectWorkflowExecutionQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useCallback, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';

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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const copilotEnabled = ai.copilot.enabled;

    const {data: workflowExecution, isLoading: workflowExecutionLoading} = useGetProjectWorkflowExecutionQuery(
        {
            id: workflowExecutionId,
        },
        workflowExecutionSheetOpen
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
            parameters: {
                environmentId: workflowExecution?.projectDeployment?.environmentId,
                workflowExecutionId,
                workflowId: workflowExecution?.job?.workflowId,
                workspaceId: currentWorkspaceId,
            },
            source: Source.WORKFLOW_EXECUTION,
        });

        setCopilotPanelOpen(true);
    }, [currentWorkspaceId, setContext, workflowExecution, workflowExecutionId]);

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
        workflowExecutionId,
        workflowExecutionLoading,
        workflowExecutionSheetOpen,
    };
};

export default useWorkflowExecutionSheet;
