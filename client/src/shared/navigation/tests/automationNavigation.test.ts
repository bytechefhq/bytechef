import {filterAutomationNavigation} from '@/shared/navigation/automationNavigation';
import {EditionType} from '@/shared/stores/useApplicationInfoStore';
import {describe, expect, it} from 'vitest';

const baseFilter = {
    aiHubEnabled: true,
    apiPlatformEnabled: false,
    contextStoreEnabled: false,
    currentEnvironmentId: 0,
    edition: EditionType.EE,
    knowledgeBaseEnabled: false,
};

describe('filterAutomationNavigation', () => {
    it('puts the AI Hub entry first on the EE edition with the AI Hub enabled', () => {
        const navigation = filterAutomationNavigation(baseFilter);

        expect(navigation[0]).toMatchObject({href: '/automation/ai-hub', name: 'AI Hub'});
        expect(navigation[1].href).toBe('/automation/projects');
    });

    it('drops the AI Hub entry when the AI Hub is disabled', () => {
        const navigation = filterAutomationNavigation({...baseFilter, aiHubEnabled: false});

        expect(navigation.map((navItem) => navItem.href)).not.toContain('/automation/ai-hub');
    });

    it('drops the AI Hub entry on the CE edition and keeps Projects first', () => {
        const navigation = filterAutomationNavigation({...baseFilter, edition: EditionType.CE});

        expect(navigation.map((navItem) => navItem.href)).not.toContain('/automation/ai-hub');
        expect(navigation[0].href).toBe('/automation/projects');
    });

    it('drops the AI Hub entry while the edition is not loaded yet', () => {
        const navigation = filterAutomationNavigation({...baseFilter, edition: undefined});

        expect(navigation.map((navItem) => navItem.href)).not.toContain('/automation/ai-hub');
    });

    it('drops the Project Deployments entry in the development environment', () => {
        const hrefs = (currentEnvironmentId: number) =>
            filterAutomationNavigation({...baseFilter, currentEnvironmentId}).map((navItem) => navItem.href);

        expect(hrefs(0)).not.toContain('/automation/deployments');
        expect(hrefs(2)).toContain('/automation/deployments');
    });

    it('shows the Chats entry only on the CE edition', () => {
        const hrefs = (edition: EditionType | undefined, aiHubEnabled: boolean) =>
            filterAutomationNavigation({...baseFilter, aiHubEnabled, edition}).map((navItem) => navItem.href);

        expect(hrefs(EditionType.CE, false)).toContain('/automation/chats');
        expect(hrefs(EditionType.EE, true)).not.toContain('/automation/chats');
        expect(hrefs(EditionType.EE, false)).not.toContain('/automation/chats');
        expect(hrefs(undefined, false)).not.toContain('/automation/chats');
    });

    it('shows the Context Store entry only on the EE edition with the context store enabled', () => {
        const hrefs = (edition: EditionType | undefined, contextStoreEnabled: boolean) =>
            filterAutomationNavigation({...baseFilter, contextStoreEnabled, edition}).map((navItem) => navItem.href);

        expect(hrefs(EditionType.EE, true)).toContain('/automation/context-stores');
        expect(hrefs(EditionType.EE, false)).not.toContain('/automation/context-stores');
        expect(hrefs(EditionType.CE, true)).not.toContain('/automation/context-stores');
    });
});
