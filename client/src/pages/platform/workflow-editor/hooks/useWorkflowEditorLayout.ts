import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/shallow';

const useWorkflowEditorLayout = () => {
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const {setClusterElementsCanvasOpen, setRootClusterElementNodeData} = useWorkflowEditorStore(
        useShallow((state) => ({
            setClusterElementsCanvasOpen: state.setClusterElementsCanvasOpen,
            setRootClusterElementNodeData: state.setRootClusterElementNodeData,
        }))
    );

    const isMainRootClusterElement = useMemo(
        () => currentNode?.clusterRoot && !currentNode?.isNestedClusterRoot,
        [currentNode?.clusterRoot, currentNode?.isNestedClusterRoot]
    );

    const handleClusterElementsCanvasOpenChange = (open: boolean) => {
        setClusterElementsCanvasOpen(open);

        if (!open) {
            setRootClusterElementNodeData(undefined);
        }
    };

    useEffect(() => {
        if (isMainRootClusterElement) {
            setRootClusterElementNodeData(currentNode);
        }
    }, [isMainRootClusterElement, setRootClusterElementNodeData, currentNode]);

    return {
        handleClusterElementsCanvasOpenChange,
        isMainRootClusterElement,
    };
};

export default useWorkflowEditorLayout;
