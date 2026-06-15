type DynamicPropertiesQueryEnabledCandidateType = {
    clusterElementContext: boolean;
    connectionId?: number;
    connections?: Array<unknown>;
};

// Gates the DYNAMIC_PROPERTIES query: enable it when a cluster element context is present, when the
// node has a configured connection, or when the node needs no connection at all.
//
// A connectionless component (e.g. workflow/responseToWorkflowCall) is served with `connections: []`
// when the workflow is loaded from the server, but a freshly added node never gets the field set, so
// `connections` is `undefined` until the next reload. Checking `connections.length === 0` therefore
// only enabled the query after a refresh, which is why schema-derived properties (e.g. the Output
// Schema "Message" field) did not show up until the page was reloaded. Treat a missing `connections`
// field the same as an empty one so the query fires immediately on first add.
export default function isDynamicPropertiesQueryEnabled({
    clusterElementContext,
    connectionId,
    connections,
}: DynamicPropertiesQueryEnabledCandidateType): boolean {
    return clusterElementContext || !!(connectionId && connections?.length) || (connections?.length ?? 0) === 0;
}
