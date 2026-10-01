import {beforeEach, describe, expect, it} from 'vitest';

import useClusterElementsViewModeStore from './useClusterElementsViewModeStore';

describe('useClusterElementsViewModeStore', () => {
    beforeEach(() => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});
    });

    it('defaults to the dialog editor', () => {
        expect(useClusterElementsViewModeStore.getState().clusterElementsViewMode).toBe('dialog');
    });

    it('switches to box mode', () => {
        useClusterElementsViewModeStore.getState().setClusterElementsViewMode('box');

        expect(useClusterElementsViewModeStore.getState().clusterElementsViewMode).toBe('box');
    });

    it('persists under a stable storage key', () => {
        useClusterElementsViewModeStore.getState().setClusterElementsViewMode('box');

        expect(JSON.parse(window.localStorage.getItem('bytechef.cluster-elements-view-mode')!).state).toEqual({
            clusterElementsViewMode: 'box',
        });
    });
});
