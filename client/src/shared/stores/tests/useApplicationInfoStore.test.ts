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
            expect(ai.knowledgeBase.enabled).toBe(false);
        });

        it('defaults custom component Java uploads to enabled before getApplicationInfo resolves', () => {
            const {component} = applicationInfoStore.getState();

            expect(component.customComponent.javaEnabled).toBe(true);
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

        it('shares the in-flight request between concurrent callers', async () => {
            const fetchSpy = mockActuatorInfo({...ACTUATOR_INFO, application: {edition: 'EE'}});

            const firstRequest = applicationInfoStore.getState().getApplicationInfo();
            const secondRequest = applicationInfoStore.getState().getApplicationInfo();

            expect(secondRequest).toBe(firstRequest);

            await Promise.all([firstRequest, secondRequest]);

            expect(fetchSpy).toHaveBeenCalledTimes(1);
            expect(applicationInfoStore.getState().application).toEqual({edition: 'EE'});
        });

        it('starts a new request once the previous one has settled', async () => {
            const fetchSpy = vi
                .spyOn(globalThis, 'fetch')
                .mockImplementation(() => Promise.resolve(new Response(JSON.stringify(ACTUATOR_INFO), {status: 200})));

            await applicationInfoStore.getState().getApplicationInfo();
            await applicationInfoStore.getState().getApplicationInfo();

            expect(fetchSpy).toHaveBeenCalledTimes(2);
        });

        it('parses authorizationServer enabled from actuator info', async () => {
            mockActuatorInfo({...ACTUATOR_INFO, oauth2: {authorizationServer: {enabled: 'true'}}});

            await applicationInfoStore.getState().getApplicationInfo();

            expect(applicationInfoStore.getState().oauth2.authorizationServer.enabled).toBe(true);
        });

        it('defaults authorizationServer to false when the field is missing', async () => {
            mockActuatorInfo(ACTUATOR_INFO);

            await applicationInfoStore.getState().getApplicationInfo();

            expect(applicationInfoStore.getState().oauth2.authorizationServer.enabled).toBe(false);
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
});
