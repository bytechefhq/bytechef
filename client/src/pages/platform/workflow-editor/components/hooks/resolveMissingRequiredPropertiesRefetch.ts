export type MissingRequiredPropertiesRefetchTargetType = 'cluster' | 'none' | 'regular';

/**
 * Picks which missing-required-properties query to imperatively refetch for the focused node ('cluster' |
 * 'regular'), or 'none'. refetch() bypasses the declarative queries' `enabled` guards, so this replicates
 * them: returns 'none' during the cluster-editor-close race (currentNodeName lags clusterElementType) and
 * for a freshly added node still awaiting its first save — both would 404 against the stale server definition.
 */
export function resolveMissingRequiredPropertiesRefetch(
    currentNodeName: string | undefined,
    currentClusterElementName: string | undefined,
    currentNodeClusterElementType: string | undefined,
    pendingSaveNodeName: string | undefined
): MissingRequiredPropertiesRefetchTargetType {
    if (!currentNodeName || currentNodeName === 'manual') {
        return 'none';
    }

    if (currentNodeName === pendingSaveNodeName) {
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
