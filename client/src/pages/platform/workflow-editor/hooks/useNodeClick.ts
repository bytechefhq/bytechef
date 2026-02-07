import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
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
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                setActiveTab: state.setActiveTab,
                setCurrentComponent: state.setCurrentComponent,
                setCurrentNode: state.setCurrentNode,
                setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
            }))
        );
    const setRightSidebarOpen = useRightSidebarStore((state) => state.setRightSidebarOpen);
    const setWorkflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.setWorkflowTestChatPanelOpen);

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
        setWorkflowTestChatPanelOpen(false);
        setActiveTab(activeTab ?? 'description');
        setCurrentNode({...data, description: ''});

        if (!!data.clusterRoot && !clusterElementsCanvasOpen) {
            setClusterElementsCanvasOpen(true);
        }

        setWorkflowNodeDetailsPanelOpen(true);

        if (data.type) {
            setCurrentComponent((previousCurrentComponent) => ({
                ...data,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: data.name,
            }));
        }
    }, [
        nodes,
        clusterElementsCanvasNodes,
        clusterElementsCanvasOpen,
        setRightSidebarOpen,
        setWorkflowTestChatPanelOpen,
        setActiveTab,
        activeTab,
        setCurrentNode,
        data,
        id,
        setClusterElementsCanvasOpen,
        setCurrentComponent,
        setWorkflowNodeDetailsPanelOpen,
    ]);
}
