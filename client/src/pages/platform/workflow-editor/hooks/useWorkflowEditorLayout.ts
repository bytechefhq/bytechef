import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/shallow';

const useWorkflowEditorLayout = () => {
    const seededRootNodeNameRef = useRef<string | undefined>(undefined);

    const workflowId = useWorkflowDataStore((state) => state.workflow.id);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);
    const {setClusterElementsCanvasOpen, setNestedClusterRootsComponentDefinitions, setRootClusterElementNodeData} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                setClusterElementsCanvasOpen: state.setClusterElementsCanvasOpen,
                setNestedClusterRootsComponentDefinitions: state.setNestedClusterRootsComponentDefinitions,
                setRootClusterElementNodeData: state.setRootClusterElementNodeData,
            }))
        );

    const isMainRootClusterElement = useMemo(
        () => currentNode?.clusterRoot && !currentNode?.isNestedClusterRoot,
        [currentNode?.clusterRoot, currentNode?.isNestedClusterRoot]
    );

    const handleClusterElementsCanvasOpenChange = useCallback(
        (open: boolean) => {
            setClusterElementsCanvasOpen(open);

            if (!open) {
                seededRootNodeNameRef.current = undefined;

                setRootClusterElementNodeData(undefined);
                setNestedClusterRootsComponentDefinitions({});
            }
        },
        [setClusterElementsCanvasOpen, setNestedClusterRootsComponentDefinitions, setRootClusterElementNodeData]
    );

    useEffect(() => {
        if (!clusterElementsCanvasOpen || !isMainRootClusterElement) {
            return;
        }

        if (seededRootNodeNameRef.current === currentNode?.workflowNodeName) {
            return;
        }

        seededRootNodeNameRef.current = currentNode?.workflowNodeName;

        setRootClusterElementNodeData(currentNode);
    }, [clusterElementsCanvasOpen, currentNode, isMainRootClusterElement, setRootClusterElementNodeData]);

    useEffect(() => {
        handleClusterElementsCanvasOpenChange(false);
    }, [handleClusterElementsCanvasOpenChange, workflowId]);

    return {
        handleClusterElementsCanvasOpenChange,
        isMainRootClusterElement,
    };
};

export default useWorkflowEditorLayout;
