import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';

import {useNodeDetailsDialogStore} from '../stores/useNodeDetailsDialogStore';

export default function useNodeClick(
    data: NodeProps['data'],
    id: NodeProps['id']
) {
    const {setCurrentNode, setNodeDetailsDialogOpen} =
        useNodeDetailsDialogStore();
    const {setRightSidebarOpen} = useRightSidebarStore();

    const {getNode} = useReactFlow();

    return useCallback(() => {
        const currentNode = getNode(id);

        if (!currentNode) {
            return;
        }

        setRightSidebarOpen(false);

        setNodeDetailsDialogOpen(true);

        setCurrentNode(data);
    }, [
        data,
        getNode,
        id,
        setCurrentNode,
        setNodeDetailsDialogOpen,
        setRightSidebarOpen,
    ]);
}
