import {beforeEach, describe, expect, it} from 'vitest';

import useAiHubSettingsStore from '../useAiHubSettingsStore';

describe('useAiHubSettingsStore', () => {
    beforeEach(() => {
        useAiHubSettingsStore.setState({artifactsCardCollapsed: false, artifactsCardHidden: false});
    });

    it('toggles the artifacts card collapsed flag', () => {
        useAiHubSettingsStore.getState().setArtifactsCardCollapsed(true);

        expect(useAiHubSettingsStore.getState().artifactsCardCollapsed).toBe(true);
    });

    it('toggles the artifacts card hidden flag', () => {
        useAiHubSettingsStore.getState().setArtifactsCardHidden(true);

        expect(useAiHubSettingsStore.getState().artifactsCardHidden).toBe(true);
    });
});
