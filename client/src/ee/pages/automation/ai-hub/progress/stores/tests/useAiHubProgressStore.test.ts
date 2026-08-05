import {aiHubProgressStore} from '@/ee/pages/automation/ai-hub/progress/stores/useAiHubProgressStore';
import {beforeEach, describe, expect, it} from 'vitest';

describe('aiHubProgressStore', () => {
    beforeEach(() => {
        aiHubProgressStore.setState({progressText: null});
    });

    it('starts with no progress text', () => {
        expect(aiHubProgressStore.getState().progressText).toBeNull();
    });

    it('setProgress stores formatted progress text', () => {
        aiHubProgressStore.getState().setProgress('research', 'Searching the web…');

        expect(aiHubProgressStore.getState().progressText).toBe('research: Searching the web…');
    });

    it('clearProgress resets to null', () => {
        aiHubProgressStore.getState().setProgress('data_analyst', 'Querying tables');
        aiHubProgressStore.getState().clearProgress();

        expect(aiHubProgressStore.getState().progressText).toBeNull();
    });

    it('setProgress overwrites previous text', () => {
        aiHubProgressStore.getState().setProgress('research', 'First call');
        aiHubProgressStore.getState().setProgress('workflow_builder', 'Second call');

        expect(aiHubProgressStore.getState().progressText).toBe('workflow_builder: Second call');
    });
});
