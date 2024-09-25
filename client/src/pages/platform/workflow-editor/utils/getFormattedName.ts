import {Node} from 'reactflow';

export default function getFormattedName(itemName: string, nodes: Node[]): string {
    const nodeNames = nodes.map((node) => node.data.name);

    const existingNodes = nodeNames.filter((name) => name?.includes(itemName));

    if (!existingNodes.length) {
        return itemName;
    }

    const existingNodeNumbers = existingNodes.map((name) => {
        const nodeNameSplit = name.split('_');

        return parseInt(nodeNameSplit[nodeNameSplit.length - 1]);
    });

    const maxExistingNodeNumber = Math.max(...existingNodeNumbers);

    return `${itemName}_${maxExistingNodeNumber + 1}`;
}
