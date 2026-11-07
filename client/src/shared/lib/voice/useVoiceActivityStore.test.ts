import {beforeEach, describe, expect, it} from 'vitest';

import {voiceActivityStore} from './useVoiceActivityStore';

describe('voiceActivityStore', () => {
    beforeEach(() => {
        voiceActivityStore.getState().reset();
    });

    it('ignores activity and end reasons from a session that has since been replaced', () => {
        const firstGeneration = voiceActivityStore.getState().beginSession();
        const secondGeneration = voiceActivityStore.getState().beginSession();

        voiceActivityStore.getState().setActivity({tool: 'lookupOrder'}, firstGeneration);
        voiceActivityStore.getState().setEndReason('silence_timeout', firstGeneration);

        expect(voiceActivityStore.getState().activity).toBe('idle');
        expect(voiceActivityStore.getState().endReason).toBeNull();

        voiceActivityStore.getState().setActivity({tool: 'lookupOrder'}, secondGeneration);

        expect(voiceActivityStore.getState().activity).toEqual({tool: 'lookupOrder'});
    });

    it('starts every session from a clean activity and end reason', () => {
        const firstGeneration = voiceActivityStore.getState().beginSession();

        voiceActivityStore.getState().setActivity('reconnecting', firstGeneration);
        voiceActivityStore.getState().setEndReason('provider_closed', firstGeneration);

        voiceActivityStore.getState().beginSession();

        expect(voiceActivityStore.getState().activity).toBe('idle');
        expect(voiceActivityStore.getState().endReason).toBeNull();
    });
});
