import {Node} from 'reactflow';

export default function getFormattedName(
    itemName: string,
    nodes: Node[]
): string {
    const nodeNames = nodes.map((node) => node.data.name);

    const existingNodes = nodeNames.filter((name) => name?.includes(itemName));

    const formattedName = existingNodes.length
        ? `${itemName}-${existingNodes.length}`
        : itemName;

    return formattedName;
}
