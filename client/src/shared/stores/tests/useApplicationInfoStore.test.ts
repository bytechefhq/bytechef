import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {afterEach, describe, expect, it, vi} from 'vitest';

const ACTUATOR_INFO = {
    ai: {copilot: {enabled: 'false'}},
    analytics: {enabled: 'false', postHog: {}},
    helpHub: {commandBar: {}, enabled: 'false'},
};

const mockActuatorInfo = (info: Record<string, unknown>) =>
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(info), {status: 200}));

describe('applicationInfoStore', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('initial state', () => {
        it('exposes a fully-shaped ai object before getApplicationInfo resolves', () => {
            const {ai} = applicationInfoStore.getState();

            expect(ai.copilot.enabled).toBe(false);
            expect(ai.hub.enabled).toBe(false);
            expect(ai.knowledgeBase.enabled).toBe(false);
        });

        it('starts without docs URLs before getApplicationInfo resolves', () => {
            expect(applicationInfoStore.getInitialState().docs).toEqual({
                changelogUrl: undefined,
                communitySupportUrl: undefined,
                documentationUrl: undefined,
                reportBugUrl: undefined,
            });
        });
    });

    describe('getApplicationInfo', () => {
        it('maps the docs URLs from the actuator info', async () => {
            mockActuatorInfo({
                ...ACTUATOR_INFO,
                docs: {
                    changelogUrl: 'https://changelog.example.com',
                    communitySupportUrl: 'https://community.example.com',
                    documentationUrl: 'https://docs.example.com',
                    reportBugUrl: 'https://bugs.example.com',
                },
            });

            await applicationInfoStore.getState().getApplicationInfo();

            expect(applicationInfoStore.getState().docs).toEqual({
                changelogUrl: 'https://changelog.example.com',
                communitySupportUrl: 'https://community.example.com',
                documentationUrl: 'https://docs.example.com',
                reportBugUrl: 'https://bugs.example.com',
            });
        });

        it('leaves blank or missing docs URLs undefined', async () => {
            mockActuatorInfo({
                ...ACTUATOR_INFO,
                docs: {changelogUrl: '', documentationUrl: 'https://docs.example.com'},
            });

            await applicationInfoStore.getState().getApplicationInfo();

            expect(applicationInfoStore.getState().docs).toEqual({
                changelogUrl: undefined,
                communitySupportUrl: undefined,
                documentationUrl: 'https://docs.example.com',
                reportBugUrl: undefined,
            });
        });
    });

    describe('ai.hub.enabled', () => {
        const loadAi = async (ai: Record<string, unknown>) => {
            mockActuatorInfo({...ACTUATOR_INFO, ai});

            await applicationInfoStore.getState().getApplicationInfo();

            return applicationInfoStore.getState().ai;
        };

        it('is true when the hub flag and copilot are both on', async () => {
            const ai = await loadAi({copilot: {enabled: 'true'}, hub: {enabled: 'true'}});

            expect(ai.hub.enabled).toBe(true);
        });

        it('is false when the hub flag is on and copilot is off', async () => {
            const ai = await loadAi({copilot: {enabled: 'false'}, hub: {enabled: 'true'}});

            expect(ai.hub.enabled).toBe(false);
        });

        it('is false when the hub flag is off and copilot is on', async () => {
            const ai = await loadAi({copilot: {enabled: 'true'}, hub: {enabled: 'false'}});

            expect(ai.hub.enabled).toBe(false);
        });

        it('is false when the hub flag is absent', async () => {
            const ai = await loadAi({copilot: {enabled: 'true'}});

            expect(ai.hub.enabled).toBe(false);
        });
    });
});
