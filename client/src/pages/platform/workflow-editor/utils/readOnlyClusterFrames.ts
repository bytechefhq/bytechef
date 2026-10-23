import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {twMerge} from 'tailwind-merge';

interface ClusterRootTaskI {
    clusterRoot?: boolean;
    name: string;
}

function toNonInteractiveNode(node: Node): Node {
    return {
        ...node,
        className: twMerge(node.className, 'pointer-events-none'),
        connectable: false,
        deletable: false,
        draggable: false,
        focusable: false,
        selectable: false,
    };
}

/**
 * The cluster roots a read-only canvas draws as frames: every root when the canvas shows a stored execution, so its
 * called elements can be coloured, and none on any other read-only canvas, which keeps the compact icon row.
 */
export function getReadOnlyFramedClusterRootIds(
    tasks: ClusterRootTaskI[] | undefined,
    hasExecutionOverlay: boolean
): string[] {
    if (!hasExecutionOverlay) {
        return [];
    }

    return (tasks ?? []).filter((task) => task.clusterRoot).map((task) => task.name);
}

export function toReadOnlyClusterFrameMembers(
    memberNodes: Node[],
    memberEdges: Edge[]
): {memberEdges: Edge[]; memberNodes: Node[]} {
    const placeholderNodeIds = new Set(
        memberNodes.filter((node) => node.type === 'placeholder').map((node) => node.id)
    );

    return {
        memberEdges: memberEdges.filter(
            (edge) => !placeholderNodeIds.has(edge.target) && !placeholderNodeIds.has(edge.source)
        ),
        memberNodes: memberNodes
            .filter((node) => !placeholderNodeIds.has(node.id))
            .map((node) => ({...toNonInteractiveNode(node), type: 'readonlyClusterElement'})),
    };
}

export function toReadOnlyFramedClusterRoot(node: Node, disabledTaskNames: Set<string>): Node {
    const nonInteractiveNode = toNonInteractiveNode(node);
    const nodeData = node.data as NodeDataType;

    return {
        ...nonInteractiveNode,
        data: {
            ...nodeData,
            isEffectivelyDisabled: Boolean(nodeData.disabled) || disabledTaskNames.has(nodeData.workflowNodeName),
        },
    };
}
