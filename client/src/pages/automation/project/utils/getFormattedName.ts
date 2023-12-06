import {Node} from 'reactflow';

export default function getFormattedName(itemName: string, nodes: Node[]): string {
    const nodeNames = nodes.map((node) => node.data.name);

    const existingNodes = nodeNames.filter((name) => name?.includes(itemName));

    const formattedName = existingNodes.length ? `${itemName}-${existingNodes.length + 1}` : `${itemName}-1`;

    return formattedName;
}
