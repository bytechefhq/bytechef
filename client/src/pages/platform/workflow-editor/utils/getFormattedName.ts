import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

export default function getFormattedName(itemName: string, nodes: Node[]): string {
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
