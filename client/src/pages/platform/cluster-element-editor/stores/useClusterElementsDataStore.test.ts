import {beforeEach, describe, expect, it} from 'vitest';

import useClusterElementsDataStore from './useClusterElementsDataStore';

describe('useClusterElementsDataStore - nodesLocked', () => {
    beforeEach(() => {
        useClusterElementsDataStore.setState({nodesLocked: true});
    });

    it('defaults nodesLocked to true', () => {
        expect(useClusterElementsDataStore.getInitialState().nodesLocked).toBe(true);
    });

    it('setNodesLocked updates the value', () => {
        useClusterElementsDataStore.getState().setNodesLocked(false);

        expect(useClusterElementsDataStore.getState().nodesLocked).toBe(false);
    });

    it('reset() returns nodesLocked to true', () => {
        useClusterElementsDataStore.getState().setNodesLocked(false);
        useClusterElementsDataStore.getState().reset();

        expect(useClusterElementsDataStore.getState().nodesLocked).toBe(true);
    });
});
