import {beforeEach, describe, expect, it} from 'vitest';

import {gettingStartedStore, isGettingStartedCompleted} from '../useGettingStartedStore';

describe('useGettingStartedStore', () => {
    beforeEach(() => {
        localStorage.clear();

        gettingStartedStore.setState({completions: {}});
    });

    it('stores a completion per platform', () => {
        gettingStartedStore.getState().complete('automation', 'DISMISSED');

        expect(isGettingStartedCompleted('automation')).toBe(true);
        expect(isGettingStartedCompleted('embedded')).toBe(false);
    });

    it('keeps the first completion', () => {
        gettingStartedStore.getState().complete('embedded', 'DISMISSED');
        gettingStartedStore.getState().complete('embedded', 'COMPLETED');

        expect(gettingStartedStore.getState().completions.embedded).toBe('DISMISSED');
    });

    it('persists only the completions to local storage', () => {
        gettingStartedStore.getState().complete('automation', 'COMPLETED');

        const storedValue = JSON.parse(localStorage.getItem('bytechef.getting-started') ?? '{}');

        expect(storedValue.state).toEqual({completions: {automation: 'COMPLETED'}});
    });
});
