import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback} from 'react';

interface UseClusterElementsCanvasDialogProps {
    onOpenChange: (open: boolean) => void;
}

export default function useClusterElementsCanvasDialog({onOpenChange}: UseClusterElementsCanvasDialogProps) {
    const {setCopilotPanelOpen} = useClusterElementsCanvasDialogStore();

    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const copilotEnabled = ai.copilot.enabled && ff_1570;

    const handleCopilotClick = useCallback(() => {
        const {context: currentContext, generateConversationId, resetMessages} = useCopilotStore.getState();

        resetMessages();
        generateConversationId();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [setContext, setCopilotPanelOpen]);

    const handleCopilotClose = useCallback(() => {
        setCopilotPanelOpen(false);
    }, [setCopilotPanelOpen]);

    const handleOpenChange = (isOpen: boolean) => {
        onOpenChange(isOpen);

        if (!isOpen) {
            useClusterElementsCanvasDialogStore.getState().reset();
            useWorkflowNodeDetailsPanelStore.getState().reset();
        }
    };

    return {
        copilotEnabled,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
    };
}
