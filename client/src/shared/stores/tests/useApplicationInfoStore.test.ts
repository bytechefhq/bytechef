import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {describe, expect, it} from 'vitest';

describe('applicationInfoStore', () => {
    describe('initial state', () => {
        it('exposes a fully-shaped ai object before getApplicationInfo resolves', () => {
            const {ai} = applicationInfoStore.getState();

            expect(ai.copilot.enabled).toBe(false);
            expect(ai.knowledgeBase.enabled).toBe(false);
        });
    });
});
