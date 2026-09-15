import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {resolveMainClusterRootName} from '@/pages/platform/workflow-editor/utils/resolveClusterRootId';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {
    useStopScriptTestMutation,
    useTestClusterElementScriptMutation,
    useTestWorkflowNodeScriptMutation,
} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface UsePropertyCodeEditorDialogToolbarProps {
    language: string;
    onChange: (value: string | undefined) => void;
    workflowId: string;
    workflowNodeName: string;
}

export const usePropertyCodeEditorDialogToolbar = ({
    language,
    onChange,
    workflowId,
    workflowNodeName,
}: UsePropertyCodeEditorDialogToolbarProps) => {
    const scriptExecutionIdRef = useRef<string | undefined>(undefined);

    const {
        dirty,
        editorValue,
        inputParameters,
        saving,
        scriptIsRunning,
        setConversationToken,
        setCopilotPanelOpen,
        setSaving,
        setScriptIsRunning,
        setScriptTestExecution,
    } = usePropertyCodeEditorDialogStore(
        useShallow((state) => ({
            dirty: state.dirty,
            editorValue: state.editorValue,
            inputParameters: state.inputParameters,
            saving: state.saving,
            scriptIsRunning: state.scriptIsRunning,
            setConversationToken: state.setConversationToken,
            setCopilotPanelOpen: state.setCopilotPanelOpen,
            setSaving: state.setSaving,
            setScriptIsRunning: state.setScriptIsRunning,
            setScriptTestExecution: state.setScriptTestExecution,
        }))
    );

    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const rootClusterElementNodeData = useWorkflowEditorStore(useShallow((state) => state.rootClusterElementNodeData));

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const readOnly = useWorkflowEditorReadOnly();

    const copilotEnabled = ai.copilot.enabled && ff_1570;

    const stopScriptTestMutation = useStopScriptTestMutation();
    const testClusterElementScriptMutation = useTestClusterElementScriptMutation();
    const testWorkflowNodeScriptMutation = useTestWorkflowNodeScriptMutation();

    const handleCopilotClick = useCallback(() => {
        const {
            context: currentContext,
            generateConversationId,
            resetMessages,
            saveConversationState,
        } = useCopilotStore.getState();

        setConversationToken(saveConversationState());

        resetMessages();
        generateConversationId();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {language},
            source: Source.CODE_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [language, setContext, setConversationToken, setCopilotPanelOpen]);

    const handleRunClick = useCallback(() => {
        if (readOnly) {
            return;
        }

        setScriptIsRunning(true);

        const mainClusterRootName = resolveMainClusterRootName(currentNode, rootClusterElementNodeData);
        const isClusterElement = currentNode?.clusterElementType && mainClusterRootName;

        if (isClusterElement) {
            testClusterElementScriptMutation
                .mutateAsync({
                    clusterElementType: currentNode.clusterElementType!,
                    clusterElementWorkflowNodeName: currentNode.name,
                    environmentId: currentEnvironmentId!,
                    executionId,
                    inputParameters,
                    workflowId,
                    workflowNodeName: mainClusterRootName as string,
                })
                .then((result) => {
                    if (process.env.NODE_ENV === 'development') {
                        console.log('[usePropertyCodeEditorDialogToolbar] testClusterElementScript result:', result);
                    }

                    setScriptTestExecution(result.testClusterElementScript);
                    setScriptIsRunning(false);
                })
                .catch((error) => {
                    if (process.env.NODE_ENV === 'development') {
                        console.error('[usePropertyCodeEditorDialogToolbar] testClusterElementScript error:', error);
                    }

                    setScriptTestExecution({
                        error: {message: error.message || 'An error occurred while executing the script'},
                    });
                    setScriptIsRunning(false);
                });
        } else {
            testWorkflowNodeScriptMutation
                .mutateAsync({
                    environmentId: currentEnvironmentId!,
                    executionId,
                    inputParameters,
                    workflowId,
                    workflowNodeName,
                })
                .then((result) => {
                    if (process.env.NODE_ENV === 'development') {
                        console.log('[usePropertyCodeEditorDialogToolbar] testWorkflowNodeScript result:', result);
                    }

                    setScriptTestExecution(result.testWorkflowNodeScript);
                    setScriptIsRunning(false);
                })
                .catch((error) => {
                    if (process.env.NODE_ENV === 'development') {
                        console.error('[usePropertyCodeEditorDialogToolbar] testWorkflowNodeScript error:', error);
                    }

                    setScriptTestExecution({
                        error: {message: error.message || 'An error occurred while executing the script'},
                    });
                    setScriptIsRunning(false);
                });
        }
    }, [
        currentEnvironmentId,
        currentNode,
        inputParameters,
        readOnly,
        rootClusterElementNodeData,
        setScriptIsRunning,
        setScriptTestExecution,
        testClusterElementScriptMutation,
        testWorkflowNodeScriptMutation,
        workflowId,
        workflowNodeName,
    ]);

    const handleSaveClick = useCallback(() => {
        if (readOnly) {
            return;
        }

        setSaving(true);

        onChange(editorValue);
    }, [editorValue, onChange, readOnly, setSaving]);

    const handleStopClick = useCallback(() => {
        const executionId = scriptExecutionIdRef.current;

        if (!executionId) {
            return;
        }

        stopScriptTestMutation.mutate({executionId});
    }, [stopScriptTestMutation]);

    return {
        copilotEnabled,
        dirty,
        handleCopilotClick,
        handleRunClick,
        handleSaveClick,
        handleStopClick,
        saving,
        scriptIsRunning,
    };
};
