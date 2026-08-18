import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {beforeEach, describe, expect, it} from 'vitest';

const DEFAULT_TABS = {automations: true, connections: true, newWorkflow: true};

describe('useAutomationHubStore', () => {
    beforeEach(() => {
        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            includeComponents: undefined,
            initialized: false,
            sharedConnectionIds: [],
            tabs: DEFAULT_TABS,
            theme: {},
        });
    });

    it('applies the params passed to initialize and flips initialized to true', () => {
        useAutomationHubStore.getState().initialize({sharedConnectionIds: [1], tabs: {connections: false}});

        const state = useAutomationHubStore.getState();

        expect(state.tabs).toEqual({automations: true, connections: false, newWorkflow: true});
        expect(state.initialized).toBe(true);
        expect(state.sharedConnectionIds).toEqual([1]);
    });

    it('keeps the defaults when initialize is called with an empty params object', () => {
        useAutomationHubStore.getState().initialize({});

        const state = useAutomationHubStore.getState();

        expect(state.connectionDialogAllowed).toBe(true);
        expect(state.includeComponents).toBeUndefined();
        expect(state.initialized).toBe(true);
        expect(state.sharedConnectionIds).toEqual([]);
        expect(state.tabs).toEqual(DEFAULT_TABS);
        expect(state.theme).toEqual({});
    });
});
