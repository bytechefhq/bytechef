import {getGraphFrameId} from './graph/graphFrameGeometry';

/**
 * Builds the bottom-ghost node id for a task dispatcher nested inside another
 * dispatcher's chain, so the enclosing chain can wire an edge from the nested
 * dispatcher's exit back into its own structure.
 *
 * The ghost node ids created by createForkJoinNode / createOnErrorNode use
 * camelCase segments ('forkJoin', 'onError') rather than the kebab-case
 * componentNames ('fork-join', 'on-error'). Deriving the segment naively from
 * `taskNodeId.split('_')[0]` therefore produces `<id>-fork-join-bottom-ghost`,
 * which matches no real node — the dagre edge is then dropped by the
 * missing-node filter, leaving the nested subtree disconnected at the bottom.
 * All other dispatchers (condition, loop, branch, each, map, parallel) use their
 * componentName verbatim, so this only remaps the two hyphenated names.
 */
const GHOST_SEGMENT_BY_COMPONENT_NAME: Record<string, string> = {
    'fork-join': 'forkJoin',
    'on-error': 'onError',
};

export function nestedDispatcherGhostSegment(componentName: string): string {
    return GHOST_SEGMENT_BY_COMPONENT_NAME[componentName] || componentName;
}

export function nestedBottomGhostIdForDispatcherTask(taskNodeId: string): string {
    const componentName = taskNodeId.split('_')[0];

    // A graph has no bottom bar — its frame is the node the enclosing chain leaves it from.
    if (componentName === 'graph') {
        return getGraphFrameId(taskNodeId);
    }

    return `${taskNodeId}-${nestedDispatcherGhostSegment(componentName)}-bottom-ghost`;
}
