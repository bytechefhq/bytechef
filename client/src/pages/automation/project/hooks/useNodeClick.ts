import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';

import {useNodeDetailsPanelStore} from '../stores/useNodeDetailsPanelStore';

export default function useNodeClick(
    data: NodeProps['data'],
    id: NodeProps['id']
) {
    const {setCurrentNode, setNodeDetailsPanelOpen} =
        useNodeDetailsPanelStore();

    const {setRightSidebarOpen} = useRightSidebarStore();

    const {getNode} = useReactFlow();

    return useCallback(() => {
        const currentNode = getNode(id);

        if (!currentNode) {
            return;
        }

        setRightSidebarOpen(false);

        setNodeDetailsPanelOpen(true);

        setCurrentNode(data);
    }, [
        data,
        getNode,
        id,
        setCurrentNode,
        setNodeDetailsPanelOpen,
        setRightSidebarOpen,
    ]);
}
