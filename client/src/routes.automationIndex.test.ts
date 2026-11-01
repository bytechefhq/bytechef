import {loadAutomationIndexRedirect} from '@/routes';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const loadedInfo = (edition: EditionType, aiHubEnabled: boolean) => ({
    ai: {copilot: {enabled: aiHubEnabled}, hub: {enabled: aiHubEnabled}, knowledgeBase: {enabled: false}},
    application: {edition},
    loading: false,
});

const redirectTarget = async () => {
    const response = (await loadAutomationIndexRedirect()) as Response;

    return response.headers.get('Location');
};

describe('loadAutomationIndexRedirect', () => {
    beforeEach(() => {
        applicationInfoStore.setState({
            ...loadedInfo(EditionType.CE, false),
            application: null,
            getApplicationInfo: vi.fn(),
        });
    });

    it('redirects to the AI Hub on the EE edition with the AI Hub enabled', async () => {
        applicationInfoStore.setState(loadedInfo(EditionType.EE, true));

        expect(await redirectTarget()).toBe('ai-hub');
    });

    it('redirects to projects on the CE edition', async () => {
        applicationInfoStore.setState(loadedInfo(EditionType.CE, true));

        expect(await redirectTarget()).toBe('projects');
    });

    it('redirects to projects on the EE edition with the AI Hub disabled', async () => {
        applicationInfoStore.setState(loadedInfo(EditionType.EE, false));

        expect(await redirectTarget()).toBe('projects');
    });

    it('fetches the application info itself when it was never requested, then decides', async () => {
        const getApplicationInfo = vi.fn(async () => {
            applicationInfoStore.setState(loadedInfo(EditionType.EE, true));
        });

        applicationInfoStore.setState({getApplicationInfo});

        expect(await redirectTarget()).toBe('ai-hub');
        expect(getApplicationInfo).toHaveBeenCalledTimes(1);
    });

    it('redirects to projects without waiting when an application info request never answers', async () => {
        const getApplicationInfo = vi.fn();

        applicationInfoStore.setState({application: null, getApplicationInfo, loading: true});

        expect(await redirectTarget()).toBe('projects');
        expect(getApplicationInfo).not.toHaveBeenCalled();
    });
});
