import {useAiAgentTestingChatStore} from '@/pages/platform/cluster-element-editor/ai-agent-editor/stores';
import {useTestingModeStore} from '@/pages/platform/cluster-element-editor/ai-agent-editor/stores/useTestingModeStore';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import useClusterElementsDataStore from '@/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore';
import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {isDataStreamSimpleModeAvailable as computeIsDataStreamSimpleModeAvailable} from '@/pages/platform/workflow-editor/utils/isDataStreamSimpleModeAvailable';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useEffect, useMemo, useRef} from 'react';

interface UseClusterElementsCanvasDialogProps {
    onOpenChange: (open: boolean) => void;
}

export default function useClusterElementsCanvasDialog({onOpenChange}: UseClusterElementsCanvasDialogProps) {
    const conversationTokenRef = useRef<string | null>(null);

    const aiAgentSimpleEditorPreferred = useClusterElementsCanvasDialogStore(
        (state) => state.aiAgentSimpleEditorPreferred
    );
    const dataStreamSimpleEditorPreferred = useClusterElementsCanvasDialogStore(
        (state) => state.dataStreamSimpleEditorPreferred
    );
    const setAiAgentSimpleEditorPreferred = useClusterElementsCanvasDialogStore(
        (state) => state.setAiAgentSimpleEditorPreferred
    );
    const setCopilotPanelOpen = useClusterElementsCanvasDialogStore((state) => state.setCopilotPanelOpen);
    const setShowAiAgentEditor = useClusterElementsCanvasDialogStore((state) => state.setShowAiAgentEditor);
    const setShowDataStreamEditor = useClusterElementsCanvasDialogStore((state) => state.setShowDataStreamEditor);
    const setDataStreamSimpleEditorPreferred = useClusterElementsCanvasDialogStore(
        (state) => state.setDataStreamSimpleEditorPreferred
    );
    const setTestingPanelOpen = useClusterElementsCanvasDialogStore((state) => state.setTestingPanelOpen);

    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const resetNodeDetailsPanel = useWorkflowNodeDetailsPanelStore((state) => state.reset);

    const isAiAgentClusterRoot = rootClusterElementNodeData?.componentName === 'aiAgent';
    const isDataStreamClusterRoot = rootClusterElementNodeData?.componentName === 'dataStream';
    const workflowNodeName = rootClusterElementNodeData?.workflowNodeName;

    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const copilotEnabled = ai.copilot.enabled && ff_1570;

    const workflow = useWorkflowDataStore((state) => state.workflow);

    const isDataStreamSimpleModeAvailable = useMemo(() => {
        if (!isDataStreamClusterRoot) {
            return true;
        }

        return computeIsDataStreamSimpleModeAvailable(workflow.definition, workflowNodeName);
    }, [isDataStreamClusterRoot, workflowNodeName, workflow.definition]);

    const handleToggleEditor = useCallback(
        (showSimpleEditor: boolean) => {
            if (isAiAgentClusterRoot) {
                setShowAiAgentEditor(showSimpleEditor);
                setAiAgentSimpleEditorPreferred(showSimpleEditor);

                useTestingModeStore.getState().resetTestingMode();

                setTestingPanelOpen(false);

                useWorkflowNodeDetailsPanelStore.getState().setAiAgentNodeDetailsPanelOpen(false);
            } else if (isDataStreamClusterRoot) {
                setShowDataStreamEditor(showSimpleEditor);
                setDataStreamSimpleEditorPreferred(showSimpleEditor);

                if (showSimpleEditor) {
                    useWorkflowNodeDetailsPanelStore.getState().reset();
                } else {
                    const panelStore = useWorkflowNodeDetailsPanelStore.getState();

                    if (rootClusterElementNodeData) {
                        panelStore.setCurrentNode((previousCurrentNode) => ({
                            ...rootClusterElementNodeData,
                            description: '',
                            displayConditions: previousCurrentNode?.displayConditions,
                        }));

                        panelStore.setWorkflowNodeDetailsPanelOpen(true);
                    }
                }
            }
        },
        [
            isAiAgentClusterRoot,
            isDataStreamClusterRoot,
            rootClusterElementNodeData,
            setAiAgentSimpleEditorPreferred,
            setDataStreamSimpleEditorPreferred,
            setShowAiAgentEditor,
            setShowDataStreamEditor,
            setTestingPanelOpen,
        ]
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
            parameters: {taskName: rootClusterElementNodeData?.name},
            source: Source.CLUSTER_ELEMENT,
        });

        setCopilotPanelOpen(true);
    }, [rootClusterElementNodeData?.name, setCopilotPanelOpen, setContext]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);
        setCopilotPanelOpen(false);
    }, [setCopilotPanelOpen]);

    const handleTestClick = useCallback(() => {
        const {generateConversationId, resetMessages} = useAiAgentTestingChatStore.getState();

        resetMessages();
        generateConversationId();
        useTestingModeStore.getState().setIsTestingAgent(true);
        setTestingPanelOpen(true);
    }, [setTestingPanelOpen]);

    const handleCloseTestingPanel = useCallback(() => {
        useTestingModeStore.getState().resetTestingMode();
        setTestingPanelOpen(false);
    }, [setTestingPanelOpen]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            onOpenChange(isOpen);

            if (!isOpen) {
                useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);
                useClusterElementsCanvasDialogStore.getState().reset();
                useClusterElementsDataStore.getState().reset();
                useTestingModeStore.getState().resetTestingMode();
                useAiAgentEvalsStore.getState().setEvalsPanelOpen(false);
                resetNodeDetailsPanel();
            }
        },
        [onOpenChange, resetNodeDetailsPanel]
    );

    const handleClose = useCallback(() => {
        handleOpenChange(false);
    }, [handleOpenChange]);

    useEffect(() => {
        setShowAiAgentEditor(isAiAgentClusterRoot && aiAgentSimpleEditorPreferred);
    }, [aiAgentSimpleEditorPreferred, isAiAgentClusterRoot, setShowAiAgentEditor]);

    useEffect(() => {
        setShowDataStreamEditor(
            isDataStreamClusterRoot && isDataStreamSimpleModeAvailable && dataStreamSimpleEditorPreferred
        );
    }, [
        dataStreamSimpleEditorPreferred,
        isDataStreamClusterRoot,
        isDataStreamSimpleModeAvailable,
        setShowDataStreamEditor,
    ]);

    const handlePointerDownOutside = useCallback((event: CustomEvent<{originalEvent: PointerEvent}>) => {
        const target = event.detail.originalEvent.target;

        if (
            target instanceof Element &&
            (target.closest('[data-sonner-toast]') || target.closest('[data-sonner-toaster]'))
        ) {
            event.preventDefault();
        }
    }, []);

    return {
        copilotEnabled,
        handleClose,
        handleCloseTestingPanel,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        handlePointerDownOutside,
        handleTestClick,
        handleToggleEditor,
        isAiAgentClusterRoot,
        isDataStreamClusterRoot,
        isDataStreamSimpleModeAvailable,
    };
}
