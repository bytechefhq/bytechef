import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
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
import {getNodeLabel} from '../utils/getNodeLabel';

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

    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
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

        const {currentNode: existingCurrentNode, workflowNodeDetailsPanelOpen: isPanelOpen} =
            useWorkflowNodeDetailsPanelStore.getState();

        const isNodeAlreadyOpen = isPanelOpen && existingCurrentNode?.workflowNodeName === data.workflowNodeName;

        setRightSidebarOpen(false);
        setWorkflowTestChatPanelOpen(false);
        setActiveTab(activeTab ?? 'description');

        if (!isNodeAlreadyOpen) {
            setDataPillPanelOpen(false);

            const {workflow} = useWorkflowDataStore.getState();

            setCurrentNode({
                ...data,
                description: '',
                label: getNodeLabel({fallbackLabel: data.label, workflow, workflowNodeName: data.workflowNodeName}),
            });

            if (!!data.clusterRoot && !clusterElementsCanvasOpen) {
                setClusterElementsCanvasOpen(true);
            }

            if (data.type) {
                setCurrentComponent((previousCurrentComponent) => ({
                    ...data,
                    displayConditions: previousCurrentComponent?.displayConditions,
                    workflowNodeName: data.name,
                }));
            }
        }

        setWorkflowNodeDetailsPanelOpen(true);
    }, [
        data,
        id,
        nodes,
        clusterElementsCanvasNodes,
        clusterElementsCanvasOpen,
        setDataPillPanelOpen,
        setRightSidebarOpen,
        setWorkflowTestChatPanelOpen,
        setActiveTab,
        activeTab,
        setCurrentNode,
        setClusterElementsCanvasOpen,
        setCurrentComponent,
        setWorkflowNodeDetailsPanelOpen,
    ]);
}
