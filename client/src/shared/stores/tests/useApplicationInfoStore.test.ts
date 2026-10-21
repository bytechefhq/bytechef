import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const actuatorInfo = (overrides: Record<string, unknown> = {}) => ({
    ai: {copilot: {enabled: 'true'}, knowledgeBase: {enabled: 'false'}},
    analytics: {enabled: 'false', postHog: {apiKey: undefined, host: undefined}},
    application: {edition: 'EE'},
    featureFlags: {},
    helpHub: {commandBar: {orgId: undefined}, enabled: 'false'},
    ...overrides,
});

const stubActuatorInfo = (json: Record<string, unknown>) => {
    vi.stubGlobal(
        'fetch',
        vi.fn(() => Promise.resolve({json: () => Promise.resolve(json), status: 200}))
    );
};

describe('applicationInfoStore', () => {
    const initialState = applicationInfoStore.getState();

    beforeEach(() => {
        applicationInfoStore.setState(initialState, true);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    describe('initial state', () => {
        it('exposes a fully-shaped ai object before getApplicationInfo resolves', () => {
            const {ai} = applicationInfoStore.getState();

            expect(ai.copilot.enabled).toBe(false);
            expect(ai.knowledgeBase.enabled).toBe(false);
        });

        it('defaults to single-tenant mode with a 30-day deletion grace period', () => {
            expect(applicationInfoStore.getState().tenant).toEqual({deletionGracePeriodDays: 30, mode: 'single'});
        });
    });

    describe('getApplicationInfo', () => {
        it('reads the tenant mode and deletion grace period', async () => {
            stubActuatorInfo(actuatorInfo({tenant: {deletionGracePeriodDays: '14', mode: 'multi'}}));

            await applicationInfoStore.getState().getApplicationInfo();

            expect(applicationInfoStore.getState().tenant).toEqual({deletionGracePeriodDays: 14, mode: 'multi'});
        });

        it('falls back to single-tenant mode and 30 days when the tenant block is missing', async () => {
            stubActuatorInfo(actuatorInfo());

            await applicationInfoStore.getState().getApplicationInfo();

            expect(applicationInfoStore.getState().tenant).toEqual({deletionGracePeriodDays: 30, mode: 'single'});
        });

        it('treats an unknown tenant mode as single-tenant', async () => {
            stubActuatorInfo(actuatorInfo({tenant: {deletionGracePeriodDays: 'not-a-number', mode: 'other'}}));

            await applicationInfoStore.getState().getApplicationInfo();

            expect(applicationInfoStore.getState().tenant).toEqual({deletionGracePeriodDays: 30, mode: 'single'});
        });
    });
});
