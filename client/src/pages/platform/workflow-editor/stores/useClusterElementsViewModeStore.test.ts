import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsViewModeStore from './useClusterElementsViewModeStore';

describe('useClusterElementsViewModeStore', () => {
    beforeEach(() => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});
    });

    it('defaults to the inline box view for a viewer with no stored preference', async () => {
        window.localStorage.removeItem('bytechef.cluster-elements-view-mode');

        vi.resetModules();

        const {default: freshStore} = await import('./useClusterElementsViewModeStore');

        expect(freshStore.getState().clusterElementsViewMode).toBe('box');
    });

    it('switches back to the dialog editor', () => {
        useClusterElementsViewModeStore.getState().setClusterElementsViewMode('dialog');

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
