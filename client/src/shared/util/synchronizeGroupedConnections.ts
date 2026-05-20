import {ComponentConnection} from '../middleware/platform/configuration';

interface SynchronizeGroupedConnectionsProps {
    componentConnections: ComponentConnection[];
    getConnectionId: (index: number) => number | undefined;
    setConnectionId: (index: number, connectionId: number | undefined) => void;
}

export function synchronizeGroupedConnections({
    componentConnections,
    getConnectionId,
    setConnectionId,
}: SynchronizeGroupedConnectionsProps): void {
    const groupIndicesByComponent = new Map<string, number[]>();

    componentConnections.forEach((componentConnection, index) => {
        const componentName = componentConnection.componentName;

        const existingComponent = groupIndicesByComponent.get(componentName);

        if (existingComponent) {
            existingComponent.push(index);
        } else {
            groupIndicesByComponent.set(componentName, [index]);
        }
    });

    groupIndicesByComponent.forEach((indices) => {
        if (indices.length < 2) {
            return;
        }

        const distinctConnectionIds = new Set<number>();

        indices.forEach((index) => {
            const connectionId = getConnectionId(index);

            if (connectionId != null) {
                distinctConnectionIds.add(connectionId);
            }
        });

        let targetConnectionId: number | undefined;

        if (distinctConnectionIds.size === 1) {
            targetConnectionId = [...distinctConnectionIds][0];
        } else if (distinctConnectionIds.size >= 2) {
            targetConnectionId = undefined;
        } else {
            return;
        }

        indices.forEach((index) => {
            if (getConnectionId(index) !== targetConnectionId) {
                setConnectionId(index, targetConnectionId);
            }
        });
    });
}
