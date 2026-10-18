import {beforeEach, describe, expect, it} from 'vitest';

import useAiHubSettingsStore from '../useAiHubSettingsStore';

describe('useAiHubSettingsStore', () => {
    beforeEach(() => {
        useAiHubSettingsStore.setState({artifactsCardCollapsed: false, showToolCalls: false});
    });

    it('hides tool calls by default and toggles them', () => {
        expect(useAiHubSettingsStore.getState().showToolCalls).toBe(false);

        useAiHubSettingsStore.getState().setShowToolCalls(true);

        expect(useAiHubSettingsStore.getState().showToolCalls).toBe(true);
    });

    it('toggles the artifacts card collapsed flag', () => {
        useAiHubSettingsStore.getState().setArtifactsCardCollapsed(true);

        expect(useAiHubSettingsStore.getState().artifactsCardCollapsed).toBe(true);
    });
});
