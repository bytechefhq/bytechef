import {CHILDLESS_TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

// Dispatchers the ELK engine lays out as compound frames (every task
// dispatcher with children). Childless dispatchers (loopBreak, subflow,
// terminate) own no children and lay out as plain chain nodes, so they are
// supported without frames.
export const ELK_FRAME_DISPATCHER_COMPONENT_NAMES = [
    'branch',
    'condition',
    'each',
    'fork-join',
    'loop',
    'map',
    'on-error',
    'parallel',
];

const SUPPORTED_DISPATCHER_COMPONENT_NAMES = new Set([
    ...CHILDLESS_TASK_DISPATCHER_NAMES,
    ...ELK_FRAME_DISPATCHER_COMPONENT_NAMES,
]);

/**
 * The experimental ELK layout engine supports every current workflow shape:
 * plain task nodes, all frame dispatchers (arbitrarily nested), childless
 * dispatchers, and cluster roots (AI agent, data stream, approval — one plain
 * chain node on the main canvas; their elements live in the separate cluster
 * elements dialog). The gate remains for future unsupported shapes: an
 * unknown dispatcher falls back to dagre and disables the toolbar switch.
 *
 * Operates on ReactFlow nodes rather than workflow tasks because dispatcher
 * children are flattened into the node array, so a single scan covers nesting.
 */
export default function isElkLayoutSupported(nodes: Node[]): boolean {
    return nodes.every((node) => {
        const nodeData = node.data as NodeDataType;

        if (nodeData.taskDispatcher && !SUPPORTED_DISPATCHER_COMPONENT_NAMES.has(nodeData.componentName)) {
            return false;
        }

        return true;
    });
}
