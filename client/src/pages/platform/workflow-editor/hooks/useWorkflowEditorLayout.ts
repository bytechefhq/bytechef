import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/shallow';

const useWorkflowEditorLayout = () => {
    const seededRootNodeNameRef = useRef<string | undefined>(undefined);

    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);
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
            seededRootNodeNameRef.current = undefined;

            setRootClusterElementNodeData(undefined);
            setMainClusterRootComponentDefinition(undefined);
            setNestedClusterRootsComponentDefinitions({});
        }
    };

    // Seeds the root data once per root while the canvas is open, and only then. currentNode is re-created by
    // saves elsewhere in the editor, and mirroring every re-creation back would overwrite a clusterElements
    // map that the dedicated writers (tool add/delete, field changes, skills) had already advanced — which is
    // how a tool added in the AI Agent editor vanished from the list until a reload. Every other field read
    // off rootClusterElementNodeData is identity (workflowNodeName, componentName, name, type, version), so
    // seeding once per root loses nothing.
    //
    // The already-seeded root is tracked in a ref rather than read back off the store: closing the canvas
    // clears the store while currentNode still points at the same root, and reading it would re-seed the data
    // the close just cleared.
    useEffect(() => {
        if (
            !clusterElementsCanvasOpen ||
            !isMainRootClusterElement ||
            seededRootNodeNameRef.current === currentNode?.workflowNodeName
        ) {
            return;
        }

        seededRootNodeNameRef.current = currentNode?.workflowNodeName;

        setRootClusterElementNodeData(currentNode);
    }, [clusterElementsCanvasOpen, currentNode, isMainRootClusterElement, setRootClusterElementNodeData]);

    return {
        handleClusterElementsCanvasOpenChange,
        isMainRootClusterElement,
    };
};

export default useWorkflowEditorLayout;
