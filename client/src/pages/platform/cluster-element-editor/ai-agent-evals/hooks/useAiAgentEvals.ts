import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import useClusterElementsViewMode from '@/pages/platform/workflow-editor/hooks/useClusterElementsViewMode';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCallback} from 'react';

export default function useAiAgentEvals() {
    const {evalsTab, setEvalsPanelOpen, setEvalsTab} = useAiAgentEvalsStore();
    const clusterElementsViewMode = useClusterElementsViewMode();
    const setClusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.setClusterElementsCanvasOpen);

    const handleClose = useCallback(() => {
        setEvalsPanelOpen(false);

        if (clusterElementsViewMode === 'box') {
            setClusterElementsCanvasOpen(false);
        }
    }, [clusterElementsViewMode, setClusterElementsCanvasOpen, setEvalsPanelOpen]);

    return {evalsTab, handleClose, setEvalsTab};
}
