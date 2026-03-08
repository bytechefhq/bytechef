import {useAiAgentTestingChatStore} from '@/pages/platform/cluster-element-editor/ai-agent-editor/stores';
import {useTestingModeStore} from '@/pages/platform/cluster-element-editor/ai-agent-editor/stores/useTestingModeStore';
import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useEffect, useMemo} from 'react';

interface UseClusterElementsCanvasDialogProps {
    onOpenChange: (open: boolean) => void;
}

export default function useClusterElementsCanvasDialog({onOpenChange}: UseClusterElementsCanvasDialogProps) {
    const setCopilotPanelOpen = useClusterElementsCanvasDialogStore((state) => state.setCopilotPanelOpen);
    const setShowAiAgentEditor = useClusterElementsCanvasDialogStore((state) => state.setShowAiAgentEditor);
    const setShowDataStreamEditor = useClusterElementsCanvasDialogStore((state) => state.setShowDataStreamEditor);
    const setEditorPreference = useClusterElementsCanvasDialogStore((state) => state.setEditorPreference);
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
        if (!isDataStreamClusterRoot || !workflowNodeName) {
            return true;
        }

        if (!workflow.definition) {
            return true;
        }

        let definition;

        try {
            definition = JSON.parse(workflow.definition);
        } catch {
            return true;
        }

        const rootTask = getTask({tasks: definition.tasks ?? [], workflowNodeName});

        if (!rootTask?.clusterElements) {
            return true;
        }

        const processorValue = rootTask.clusterElements['processor'];

        if (!processorValue) {
            return true;
        }

        const processorElement = Array.isArray(processorValue) ? processorValue[0] : processorValue;

        const typeSegments = processorElement?.type?.split('/') ?? [];
        const componentName = typeSegments[0] ?? '';
        const operationName = typeSegments[2] ?? '';

        return componentName === 'dataStreamProcessor' && operationName === 'fieldMapper';
    }, [isDataStreamClusterRoot, workflowNodeName, workflow.definition]);

    useEffect(() => {
        if (isAiAgentClusterRoot && workflowNodeName) {
            const preference = useClusterElementsCanvasDialogStore.getState().editorPreferences[workflowNodeName];

            const showAiAgent = preference ?? true;

            setShowAiAgentEditor(showAiAgent);
        }
    }, [isAiAgentClusterRoot, workflowNodeName, setShowAiAgentEditor]);

    useEffect(() => {
        if (isDataStreamClusterRoot && workflowNodeName) {
            if (!isDataStreamSimpleModeAvailable) {
                setShowDataStreamEditor(false);

                return;
            }

            const preference = useClusterElementsCanvasDialogStore.getState().editorPreferences[workflowNodeName];

            const showDataStream = preference ?? true;

            setShowDataStreamEditor(showDataStream);
        }
    }, [isDataStreamClusterRoot, isDataStreamSimpleModeAvailable, workflowNodeName, setShowDataStreamEditor]);

    const handleToggleEditor = useCallback(
        (showSimpleEditor: boolean) => {
            if (isAiAgentClusterRoot) {
                setShowAiAgentEditor(showSimpleEditor);

                useTestingModeStore.getState().resetTestingMode();

                useWorkflowNodeDetailsPanelStore.getState().setAiAgentNodeDetailsPanelOpen(false);
            } else if (isDataStreamClusterRoot) {
                setShowDataStreamEditor(showSimpleEditor);

                if (showSimpleEditor) {
                    useWorkflowNodeDetailsPanelStore.getState().reset();
                } else {
                    const panelStore = useWorkflowNodeDetailsPanelStore.getState();

                    if (rootClusterElementNodeData) {
                        panelStore.setCurrentNode({
                            ...rootClusterElementNodeData,
                            description: '',
                        });

                        panelStore.setCurrentComponent((previousComponent) => ({
                            ...rootClusterElementNodeData,
                            displayConditions: previousComponent?.displayConditions,
                            workflowNodeName: rootClusterElementNodeData.workflowNodeName || '',
                        }));

                        panelStore.setWorkflowNodeDetailsPanelOpen(true);
                    }
                }
            }

            if (workflowNodeName) {
                setEditorPreference(workflowNodeName, showSimpleEditor);
            }
        },
        [
            workflowNodeName,
            isAiAgentClusterRoot,
            isDataStreamClusterRoot,
            rootClusterElementNodeData,
            setEditorPreference,
            setShowAiAgentEditor,
            setShowDataStreamEditor,
        ]
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
    }, [setContext, setCopilotPanelOpen]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState();
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
                useCopilotStore.getState().restoreConversationState();
                useClusterElementsCanvasDialogStore.getState().reset();
                useTestingModeStore.getState().resetTestingMode();
                resetNodeDetailsPanel();
            }
        },
        [onOpenChange, resetNodeDetailsPanel]
    );

    const handleClose = useCallback(() => {
        handleOpenChange(false);
    }, [handleOpenChange]);

    useEffect(() => {
        if (isAiAgentClusterRoot && workflowNodeName) {
            const showAiAgent =
                useClusterElementsCanvasDialogStore.getState().editorPreferences[workflowNodeName] ?? true;

            setShowAiAgentEditor(showAiAgent);
        } else {
            setShowAiAgentEditor(false);
        }
    }, [workflowNodeName, isAiAgentClusterRoot, setShowAiAgentEditor]);

    useEffect(() => {
        if (isDataStreamClusterRoot && workflowNodeName) {
            const showDataStream =
                useClusterElementsCanvasDialogStore.getState().editorPreferences[workflowNodeName] ?? true;

            setShowDataStreamEditor(showDataStream);
        } else {
            setShowDataStreamEditor(false);
        }
    }, [workflowNodeName, isDataStreamClusterRoot, setShowDataStreamEditor]);

    return {
        copilotEnabled,
        handleClose,
        handleCloseTestingPanel,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        handleTestClick,
        handleToggleEditor,
        isAiAgentClusterRoot,
        isDataStreamClusterRoot,
        isDataStreamSimpleModeAvailable,
    };
}
