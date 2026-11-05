export type NodeOpenTabResolutionType = 'CONNECTION' | 'KEEP' | 'WAIT';

interface ResolveNodeOpenTabProps {
    componentDefinition?: {connectionRequired?: boolean; name?: string};
    connectionsFetched: boolean;
    nodeComponentName?: string;
    testConfigurationConnectionsCount: number;
}

export default function resolveNodeOpenTab({
    componentDefinition,
    connectionsFetched,
    nodeComponentName,
    testConfigurationConnectionsCount,
}: ResolveNodeOpenTabProps): NodeOpenTabResolutionType {
    if (!componentDefinition || componentDefinition.name !== nodeComponentName) {
        return 'WAIT';
    }

    if (!componentDefinition.connectionRequired) {
        return 'KEEP';
    }

    if (!connectionsFetched) {
        return 'WAIT';
    }

    return testConfigurationConnectionsCount > 0 ? 'KEEP' : 'CONNECTION';
}
