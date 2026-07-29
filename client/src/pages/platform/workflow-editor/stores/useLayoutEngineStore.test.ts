import {beforeEach, describe, expect, it, vi} from 'vitest';

describe('useLayoutEngineStore persistence', () => {
    beforeEach(() => {
        vi.resetModules();

        localStorage.clear();
    });

    it('defaults to elk with no persisted state', async () => {
        const {default: useLayoutEngineStore} = await import('./useLayoutEngineStore');

        expect(useLayoutEngineStore.getState().layoutEngine).toBe('elk');
    });

    it('hydrates a persisted dagre selection on load', async () => {
        localStorage.setItem('bytechef.layout-engine', JSON.stringify({state: {layoutEngine: 'dagre'}, version: 0}));

        const {default: useLayoutEngineStore} = await import('./useLayoutEngineStore');

        expect(useLayoutEngineStore.getState().layoutEngine).toBe('dagre');
    });

    it('writes the selection back to localStorage on change', async () => {
        const {default: useLayoutEngineStore} = await import('./useLayoutEngineStore');

        useLayoutEngineStore.getState().setLayoutEngine('elk');

        const persisted = JSON.parse(localStorage.getItem('bytechef.layout-engine') || '{}');

        expect(persisted.state?.layoutEngine).toBe('elk');
    });
});
