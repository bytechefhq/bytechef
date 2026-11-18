import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {describe, expect, it} from 'vitest';

import shouldShowAutomationGettingStartedHelpLink from '../shouldShowAutomationGettingStartedHelpLink';

describe('shouldShowAutomationGettingStartedHelpLink', () => {
    it('shows the help link on automation pages for users who can create projects', () => {
        expect(
            shouldShowAutomationGettingStartedHelpLink({
                canCreateProject: true,
                development: true,
                pathname: '/automation/projects',
            })
        ).toBe(true);
    });

    it('hides the help link outside automation pages', () => {
        expect(
            shouldShowAutomationGettingStartedHelpLink({
                canCreateProject: true,
                development: true,
                pathname: '/embedded/integrations',
            })
        ).toBe(false);
        expect(
            shouldShowAutomationGettingStartedHelpLink({
                canCreateProject: true,
                development: true,
                pathname: '/automation-other',
            })
        ).toBe(false);
    });

    it('hides the help link for users who cannot create projects', () => {
        expect(
            shouldShowAutomationGettingStartedHelpLink({
                canCreateProject: false,
                development: true,
                pathname: '/automation/projects',
            })
        ).toBe(false);
    });

    it('hides the help link outside Development', () => {
        expect(
            shouldShowAutomationGettingStartedHelpLink({
                canCreateProject: true,
                development: false,
                pathname: '/automation/deployments',
            })
        ).toBe(false);
    });

    it('shows the help link on Organization settings after coming from Automation', () => {
        expect(
            shouldShowAutomationGettingStartedHelpLink({
                canCreateProject: true,
                development: true,
                pathname: '/settings/users',
                storedPlatformType: PlatformType.AUTOMATION,
            })
        ).toBe(true);
    });

    it('hides the help link on Organization settings after coming from Embedded', () => {
        expect(
            shouldShowAutomationGettingStartedHelpLink({
                canCreateProject: true,
                development: true,
                pathname: '/settings/users',
                storedPlatformType: PlatformType.EMBEDDED,
            })
        ).toBe(false);
    });
});
