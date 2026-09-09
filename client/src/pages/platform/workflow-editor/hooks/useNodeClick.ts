import {NodeDataType, TabNameType} from '@/shared/types';
import {NodeProps} from '@xyflow/react';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import openNodeDetails from '../utils/openNodeDetails';

export default function useNodeClick(data: NodeDataType, id: NodeProps['id'], activeTab?: TabNameType) {
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

    const {clusterElementsCanvasOpen} = useWorkflowEditorStore();

    return useCallback(() => {
        const clickedNode = nodes.find((node) => node.id === id);
        const clickedClusterNode = clusterElementsCanvasNodes.find((node) => node.id === id);

        if (!clusterElementsCanvasOpen && !clickedNode) {
            return;
        }

        if (clusterElementsCanvasOpen && !clickedClusterNode) {
            return;
        }

        openNodeDetails(data, activeTab);
    }, [activeTab, clusterElementsCanvasNodes, clusterElementsCanvasOpen, data, id, nodes]);
}
