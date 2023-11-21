import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';

import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';

export default function useNodeClick(
    data: NodeProps['data'],
    id: NodeProps['id']
) {
    const {setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();

    const {setRightSidebarOpen} = useRightSidebarStore();

    const {getNode} = useReactFlow();

    return useCallback(() => {
        const currentNode = getNode(id);

        if (!currentNode) {
            return;
        }

        setRightSidebarOpen(false);

        setWorkflowNodeDetailsPanelOpen(true);

        setCurrentNode(data);
    }, [
        data,
        getNode,
        id,
        setCurrentNode,
        setWorkflowNodeDetailsPanelOpen,
        setRightSidebarOpen,
    ]);
}
