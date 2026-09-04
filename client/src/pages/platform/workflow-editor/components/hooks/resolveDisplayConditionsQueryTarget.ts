export type DisplayConditionsQueryTargetType = 'cluster' | 'none' | 'regular';

interface ResolveDisplayConditionsQueryTargetProps {
    activeTab: string;
    currentClusterElementName: string | undefined;
    currentNodeClusterElementType: string | undefined;
    currentNodeName: string | undefined;
    pendingSaveNodeName: string | undefined;
}

/**
 * Picks which display-conditions query the focused node may run ('cluster' | 'regular'), or 'none'.
 * Both queries resolve the node by name against the server-side workflow definition, so a freshly
 * added node still awaiting its first save must not be queried — it 400s with "Workflow node with
 * name: <name> does not exist" — and neither may the manual trigger, which is never persisted as a
 * node. The cluster/regular split mirrors resolveMissingRequiredPropertiesRefetch, including the
 * cluster-editor-close race where clusterElementType clears before the name catches up.
 */
export function resolveDisplayConditionsQueryTarget({
    activeTab,
    currentClusterElementName,
    currentNodeClusterElementType,
    currentNodeName,
    pendingSaveNodeName,
}: ResolveDisplayConditionsQueryTargetProps): DisplayConditionsQueryTargetType {
    if (activeTab !== 'properties' || !currentNodeName || currentNodeName === 'manual') {
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
