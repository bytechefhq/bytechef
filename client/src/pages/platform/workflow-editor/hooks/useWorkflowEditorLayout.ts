import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/shallow';

const useWorkflowEditorLayout = () => {
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const {
        setClusterElementsCanvasOpen,
        setMainClusterRootComponentDefinition,
        setNestedClusterRootsComponentDefinitions,
        setRootClusterElementNodeData,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            setClusterElementsCanvasOpen: state.setClusterElementsCanvasOpen,
            setMainClusterRootComponentDefinition: state.setMainClusterRootComponentDefinition,
            setNestedClusterRootsComponentDefinitions: state.setNestedClusterRootsComponentDefinitions,
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
            setMainClusterRootComponentDefinition(undefined);
            setNestedClusterRootsComponentDefinitions({});
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
