import {describe, expect, it} from 'vitest';

import {useUserGuidingStore} from '../useUserGuidingStore';

describe('useUserGuidingStore', () => {
    it('shows the guide launcher by default', () => {
        expect(useUserGuidingStore.getInitialState().launcherHidden).toBe(false);
    });

    it('remembers when the guide launcher is hidden', () => {
        useUserGuidingStore.getState().setLauncherHidden(true);

        expect(useUserGuidingStore.getState().launcherHidden).toBe(true);
    });
});
