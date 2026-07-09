export type MissingRequiredPropertiesRefetchTargetType = 'cluster' | 'none' | 'regular';

/**
 * Decides which missing-required-properties query to imperatively refetch for the focused node, or 'none'.
 *
 * Replicates the `enabled` guards of the two declarative queries in useWorkflowNodeDetailsPanel (a node is a
 * cluster element iff `currentNodeName === currentClusterElementName` AND it has a `clusterElementType`).
 * refetch() bypasses those guards, so without this the plain endpoint can fire with a cluster element's name
 * during the close/switch window where `clusterElementType` has cleared in the store but `currentNodeName`
 * still lags (local state) — the server has no task/trigger by that name and 404s. In that window both names
 * are still equal, so neither branch matches and we return 'none'; the effect re-runs once state reconciles.
 */
export function resolveMissingRequiredPropertiesRefetch(
    currentNodeName: string | undefined,
    currentClusterElementName: string | undefined,
    currentNodeClusterElementType: string | undefined
): MissingRequiredPropertiesRefetchTargetType {
    if (!currentNodeName || currentNodeName === 'manual') {
        return 'none';
    }

    if (currentNodeName === currentClusterElementName && !!currentNodeClusterElementType) {
        return 'cluster';
    }

    if (currentNodeName !== currentClusterElementName && !currentNodeClusterElementType) {
        return 'regular';
    }

    return 'none';
}
