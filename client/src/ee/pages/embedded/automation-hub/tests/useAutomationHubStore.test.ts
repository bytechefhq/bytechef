import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {beforeEach, describe, expect, it} from 'vitest';

const DEFAULT_TABS = {automations: true, connections: true, newWorkflow: true};

const createJwt = (payload: object) =>
    `header.${btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')}.signature`;

describe('useAutomationHubStore', () => {
    beforeEach(() => {
        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            includeComponents: undefined,
            initialized: false,
            tabs: DEFAULT_TABS,
            theme: {},
        });
    });

    it('applies the params passed to initialize and flips initialized to true', () => {
        useAutomationHubStore.getState().initialize({tabs: {connections: false}});

        const state = useAutomationHubStore.getState();

        expect(state.tabs).toEqual({automations: true, connections: false, newWorkflow: true});
        expect(state.initialized).toBe(true);
    });

    it('keeps the defaults when initialize is called with an empty params object', () => {
        useAutomationHubStore.getState().initialize({});

        const state = useAutomationHubStore.getState();

        expect(state.connectionDialogAllowed).toBe(true);
        expect(state.includeComponents).toBeUndefined();
        expect(state.initialized).toBe(true);
        expect(state.tabs).toEqual(DEFAULT_TABS);
        expect(state.theme).toEqual({});
    });

    it('takes connectionDialogAllowed from the token claim', () => {
        useAutomationHubStore.getState().initialize({jwtToken: createJwt({connectionDialogAllowed: false})});

        expect(useAutomationHubStore.getState().connectionDialogAllowed).toBe(false);
    });

    it('lets the deprecated connectionDialogAllowed param only remove the permission', () => {
        useAutomationHubStore
            .getState()
            .initialize({connectionDialogAllowed: true, jwtToken: createJwt({connectionDialogAllowed: false})});

        expect(useAutomationHubStore.getState().connectionDialogAllowed).toBe(false);

        useAutomationHubStore
            .getState()
            .initialize({connectionDialogAllowed: false, jwtToken: createJwt({connectionDialogAllowed: true})});

        expect(useAutomationHubStore.getState().connectionDialogAllowed).toBe(false);
    });
});
