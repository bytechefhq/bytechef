import {NodeDataType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function getFormattedName(itemName: string): string {
    const {nodes} = useWorkflowDataStore.getState();
    const nodeNames = nodes.map((node) => (node.data as NodeDataType).name);

    const existingNodes = nodeNames.filter((name) => name?.includes(itemName));

    if (!existingNodes.length) {
        return `${itemName}_1`;
    }

    const existingNodeNumbers = existingNodes.map((name) => {
        const nodeNameSplit = name.split('_');

        return parseInt(nodeNameSplit[nodeNameSplit.length - 1]);
    });

    const maxExistingNodeNumber = Math.max(...existingNodeNumbers);

    return `${itemName}_${maxExistingNodeNumber + 1}`;
}
