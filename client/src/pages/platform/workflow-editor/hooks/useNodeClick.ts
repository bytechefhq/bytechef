import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import {NodeDataType, TabNameType} from '@/shared/types';
import {NodeProps} from '@xyflow/react';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function useNodeClick(data: NodeDataType, id: NodeProps['id'], activeTab?: TabNameType) {
    const {setActiveTab, setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();
    const {setRightSidebarOpen} = useRightSidebarStore();

    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const {nodes: clusterElementsCanvasNodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const {clusterElementsCanvasOpen, setClusterElementsCanvasOpen} = useWorkflowEditorStore();

    return useCallback(() => {
        const clickedNode = nodes.find((node) => node.id === id);
        const clickedClusterNode = clusterElementsCanvasNodes.find((node) => node.id === id);

        if (!clusterElementsCanvasOpen && !clickedNode) {
            return;
        }

        if (clusterElementsCanvasOpen && !clickedClusterNode) {
            return;
        }

        setRightSidebarOpen(false);
        setActiveTab(activeTab ?? 'description');
        setCurrentNode({...data, description: ''});

        if (data.rootClusterElement && !clusterElementsCanvasOpen) {
            setClusterElementsCanvasOpen(true);

            setCurrentComponent(undefined);

            return;
        }

        setWorkflowNodeDetailsPanelOpen(true);

        if (data.type) {
            setCurrentComponent({
                ...data,
                workflowNodeName: data.name,
            });
        }
    }, [
        nodes,
        clusterElementsCanvasNodes,
        clusterElementsCanvasOpen,
        setRightSidebarOpen,
        setActiveTab,
        activeTab,
        setCurrentNode,
        data,
        id,
        setClusterElementsCanvasOpen,
        setWorkflowNodeDetailsPanelOpen,
        setCurrentComponent,
    ]);
}
