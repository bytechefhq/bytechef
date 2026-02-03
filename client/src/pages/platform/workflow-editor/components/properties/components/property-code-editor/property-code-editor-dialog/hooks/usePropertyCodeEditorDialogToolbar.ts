import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {WorkflowNodeScriptApi} from '@/shared/middleware/platform/configuration';
import {testClusterElementScript} from '@/shared/mutations/platform/clusterElementScript.mutations';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

const workflowNodeScriptApi = new WorkflowNodeScriptApi();

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
    const {
        dirty,
        editorValue,
        saving,
        scriptIsRunning,
        setCopilotPanelOpen,
        setSaving,
        setScriptIsRunning,
        setScriptTestExecution,
    } = usePropertyCodeEditorDialogStore(
        useShallow((state) => ({
            dirty: state.dirty,
            editorValue: state.editorValue,
            saving: state.saving,
            scriptIsRunning: state.scriptIsRunning,
            setCopilotPanelOpen: state.setCopilotPanelOpen,
            setSaving: state.setSaving,
            setScriptIsRunning: state.setScriptIsRunning,
            setScriptTestExecution: state.setScriptTestExecution,
        }))
    );

    const setContext = useCopilotStore((state) => state.setContext);
    const ai = useApplicationInfoStore((state) => state.ai);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const ff_1570 = useFeatureFlagsStore()('ff-1570');
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const rootClusterElementNodeData = useWorkflowEditorStore(useShallow((state) => state.rootClusterElementNodeData));

    const copilotEnabled = ai.copilot.enabled && ff_1570;

    const handleCopilotClick = useCallback(() => {
        const currentContext = useCopilotStore.getState().context;

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {language},
            source: Source.CODE_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [language, setContext, setCopilotPanelOpen]);

    const handleRunClick = useCallback(() => {
        setScriptIsRunning(true);

        const isClusterElement = currentNode?.clusterElementType && rootClusterElementNodeData?.workflowNodeName;

        if (isClusterElement) {
            testClusterElementScript({
                clusterElementType: currentNode.clusterElementType!,
                clusterElementWorkflowNodeName: currentNode.name,
                environmentId: currentEnvironmentId!,
                workflowId,
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            })
                .then((scriptTestExecution) => {
                    setScriptTestExecution(scriptTestExecution);
                    setScriptIsRunning(false);
                })
                .catch(() => {
                    setScriptIsRunning(false);
                });
        } else {
            workflowNodeScriptApi
                .testWorkflowNodeScript({
                    environmentId: currentEnvironmentId!,
                    id: workflowId,
                    workflowNodeName,
                })
                .then((scriptTestExecution) => {
                    setScriptTestExecution(scriptTestExecution);
                    setScriptIsRunning(false);
                })
                .catch(() => {
                    setScriptIsRunning(false);
                });
        }
    }, [
        currentEnvironmentId,
        currentNode,
        rootClusterElementNodeData,
        setScriptIsRunning,
        setScriptTestExecution,
        workflowId,
        workflowNodeName,
    ]);

    const handleSaveClick = useCallback(() => {
        setSaving(true);

        onChange(editorValue);
    }, [editorValue, onChange, setSaving]);

    const handleStopClick = useCallback(() => {
        console.warn('Stop functionality not yet implemented');
    }, []);

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
