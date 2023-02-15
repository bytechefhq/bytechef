import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';
import useRightSlideOverStore from '../stores/rightSlideOver.store';

// this hook implements the logic for clicking a workflow node
// on workflow node click: create a new child node of the clicked node
export default function useNodeClick(
    data: NodeProps['data'],
    id: NodeProps['id']
) {
    const {setCurrentNode, setRightSlideOverOpen} = useRightSlideOverStore();

    const {getNode} = useReactFlow();

    const onClick = useCallback(() => {
        const currentNode = getNode(id);

        if (!currentNode) {
            return;
        }

        setRightSlideOverOpen(true);

        setCurrentNode(data);
    }, [data, getNode, id, setCurrentNode, setRightSlideOverOpen]);

    return onClick;
}
