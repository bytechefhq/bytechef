import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';

import useNodeDetailsDialogStore from '../stores/useNodeDetailsDialogStore';

export default function useNodeClick(
    data: NodeProps['data'],
    id: NodeProps['id']
) {
    const {setCurrentNode, setNodeDetailsOpen} = useNodeDetailsDialogStore();

    const {getNode} = useReactFlow();

    const onClick = useCallback(() => {
        const currentNode = getNode(id);

        if (!currentNode) {
            return;
        }

        setNodeDetailsOpen(true);

        setCurrentNode(data);
    }, [data, getNode, id, setCurrentNode, setNodeDetailsOpen]);

    return onClick;
}
