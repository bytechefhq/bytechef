import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

/**
 * Phase 1 of the experimental ELK layout engine supports plain task nodes and the
 * condition task dispatcher only. Any other dispatcher (branch, each, fork-join,
 * loop, map, parallel, on-error) or an AI-agent cluster root makes the workflow
 * unsupported: layout falls back to dagre and the toolbar switch is disabled.
 *
 * Operates on ReactFlow nodes rather than workflow tasks because dispatcher
 * children are flattened into the node array, so a single scan covers nesting.
 */
export default function isElkLayoutSupported(nodes: Node[]): boolean {
    return nodes.every((node) => {
        if (node.type === 'clusterRoot') {
            return false;
        }

        const nodeData = node.data as NodeDataType;

        if (nodeData.taskDispatcher && nodeData.componentName !== 'condition') {
            return false;
        }

        return true;
    });
}
