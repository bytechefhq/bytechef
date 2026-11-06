import {ComponentConnection} from '@/shared/middleware/platform/configuration';
import {describe, expect, it, vi} from 'vitest';

import {synchronizeGroupedConnections} from '../synchronizeGroupedConnections';

function buildComponentConnection(componentName: string, key: string): ComponentConnection {
    return {componentName, componentVersion: 1, key, required: true, workflowNodeName: `${componentName}_${key}`};
}

function synchronize(componentNames: string[], initialConnectionIds: Array<number | undefined>) {
    const connectionIds = [...initialConnectionIds];
    const setConnectionId = vi.fn((index: number, connectionId: number | undefined) => {
        connectionIds[index] = connectionId;
    });

    synchronizeGroupedConnections({
        componentConnections: componentNames.map((componentName, index) =>
            buildComponentConnection(componentName, `connection_${index}`)
        ),
        getConnectionId: (index) => connectionIds[index],
        setConnectionId,
    });

    return {connectionIds, setConnectionId};
}

describe('synchronizeGroupedConnections', () => {
    it('leaves a component with a single connection alone', () => {
        const {connectionIds, setConnectionId} = synchronize(['slack', 'github'], [1, undefined]);

        expect(setConnectionId).not.toHaveBeenCalled();
        expect(connectionIds).toEqual([1, undefined]);
    });

    it('copies the only chosen connection to the rest of its group', () => {
        const {connectionIds, setConnectionId} = synchronize(['slack', 'slack', 'slack'], [undefined, 7, undefined]);

        expect(connectionIds).toEqual([7, 7, 7]);
        expect(setConnectionId).toHaveBeenCalledTimes(2);
        expect(setConnectionId).toHaveBeenCalledWith(0, 7);
        expect(setConnectionId).toHaveBeenCalledWith(2, 7);
    });

    it('does nothing when the group already agrees', () => {
        const {setConnectionId} = synchronize(['slack', 'slack'], [7, 7]);

        expect(setConnectionId).not.toHaveBeenCalled();
    });

    it('clears the whole group when it holds conflicting connections', () => {
        const {connectionIds, setConnectionId} = synchronize(['slack', 'slack', 'slack'], [7, 8, undefined]);

        expect(connectionIds).toEqual([undefined, undefined, undefined]);
        expect(setConnectionId).toHaveBeenCalledTimes(2);
        expect(setConnectionId).toHaveBeenCalledWith(0, undefined);
        expect(setConnectionId).toHaveBeenCalledWith(1, undefined);
    });

    it('does nothing when no connection in the group is chosen', () => {
        const {setConnectionId} = synchronize(['slack', 'slack'], [undefined, undefined]);

        expect(setConnectionId).not.toHaveBeenCalled();
    });

    it('treats a connection id of zero as chosen', () => {
        const {connectionIds} = synchronize(['slack', 'slack'], [0, undefined]);

        expect(connectionIds).toEqual([0, 0]);
    });

    it('synchronizes each component group independently', () => {
        const {connectionIds} = synchronize(
            ['slack', 'github', 'slack', 'github', 'jira'],
            [3, 4, undefined, 5, undefined]
        );

        expect(connectionIds).toEqual([3, undefined, 3, undefined, undefined]);
    });
});
