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

    // Seeds the root data once per root while the canvas is open. clusterElementsCanvasOpen is the honest
    // discriminator for whether the slot is wanted at all -- every consumer is canvas-scoped, and reopening on
    // an already-open agent moves no other dep, so without it the slot stays undefined and the simple editor
    // renders empty.
    //
    // Seeding only once per root is what keeps it from overwriting: currentNode is re-created by saves
    // elsewhere in the editor, and mirroring every re-creation back would clobber a clusterElements map the
    // dedicated writers (tool add/delete, field changes, skills) had already advanced -- which is how a tool
    // added in the AI Agent editor vanished from the list until a reload. Every other field read off
    // rootClusterElementNodeData is identity (workflowNodeName, componentName, name, type, version), so
    // seeding once loses nothing.
    //
    // The already-seeded root is tracked in a ref rather than read back off the store: closing the canvas
    // clears the store while currentNode still points at the same root, and reading it would re-seed the data
    // the close just cleared. The close clears the ref too, so reopening seeds again.
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

    return {
        handleClusterElementsCanvasOpenChange,
        isMainRootClusterElement,
    };
};

export default useWorkflowEditorLayout;
