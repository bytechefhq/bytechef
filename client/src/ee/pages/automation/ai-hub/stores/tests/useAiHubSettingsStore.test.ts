import {beforeEach, describe, expect, it} from 'vitest';

import useAiHubSettingsStore from '../useAiHubSettingsStore';

describe('useAiHubSettingsStore', () => {
    beforeEach(() => {
        useAiHubSettingsStore.setState({artifactsCardCollapsed: false});
    });

    it('toggles the artifacts card collapsed flag', () => {
        useAiHubSettingsStore.getState().setArtifactsCardCollapsed(true);

        expect(useAiHubSettingsStore.getState().artifactsCardCollapsed).toBe(true);
    });
});
