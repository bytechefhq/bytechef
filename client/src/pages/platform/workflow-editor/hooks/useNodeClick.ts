import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import {NodeType} from '@/shared/types';
import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function useNodeClick(data: NodeProps['data'], id: NodeProps['id']) {
    const {setCurrentNode, setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setRightSidebarOpen} = useRightSidebarStore();

    const {getNode} = useReactFlow();

    return useCallback(() => {
        const currentNode = getNode(id);

        if (!currentNode) {
            return;
        }

        let nodeData: NodeType = data;

        if (currentNode.position.y === 0) {
            nodeData = {
                ...data,
                trigger: true,
            };
        }

        setRightSidebarOpen(false);

        setWorkflowNodeDetailsPanelOpen(true);

        setCurrentNode(nodeData);
    }, [data, getNode, id, setCurrentNode, setWorkflowNodeDetailsPanelOpen, setRightSidebarOpen]);
}
