/**
 * The React Flow handle-id suffixes a graph frame's parts render and address.
 *
 * Deliberately a leaf module with no imports of its own. The node components that RENDER these
 * handles (`GraphStartNode`, `GraphTransitionHandles`) are otherwise tiny, and taking them through
 * `graphConnections` — which reaches `graphFrameGeometry` and, through it, elkjs — would drag the
 * whole layout stack into every canvas node and every test that mounts one.
 */

// The handle suffixes `GraphTransitionHandles` renders and `createGraphEdges` addresses.
export const GRAPH_TRANSITION_DYNAMIC_HANDLE_SUFFIX = '-graph-transition-dynamic';
export const GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX = '-graph-transition-source';
export const GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX = '-graph-transition-target';

// The single handle `GraphStartNode` renders, as `<startNodeId>-source`.
export const GRAPH_START_SOURCE_HANDLE_SUFFIX = '-source';
