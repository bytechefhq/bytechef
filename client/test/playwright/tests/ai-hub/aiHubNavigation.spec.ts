import {expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {AI_HUB_PATH, overrideAiApplicationInfo, prepareAiHub} from '../../utils/aiHubUtils';

export const test = mergeTests(loginTest());

const PROJECTS_PATH = '/automation/projects';

test.describe('AI Hub navigation', () => {
    test('opening /automation lands on the AI Hub, the first automation nav entry', async ({
        authenticatedPage: page,
    }) => {
        await prepareAiHub(page);

        await page.goto('/automation');

        await page.waitForURL((url) => url.pathname === AI_HUB_PATH);

        const mainNavigation = page.getByRole('navigation', {name: 'Main navigation'});

        await expect(mainNavigation.getByRole('link').first()).toHaveAttribute('href', AI_HUB_PATH);

        await expect(mainNavigation.getByRole('link', {name: 'AI Hub'})).toBeVisible();
    });

    test('with the hub off, /automation lands on Projects and there is no AI Hub entry', async ({
        authenticatedPage: page,
    }) => {
        await overrideAiApplicationInfo(page, {hubEnabled: false});

        await page.goto('/automation');

        await page.waitForURL((url) => url.pathname === PROJECTS_PATH);

        const mainNavigation = page.getByRole('navigation', {name: 'Main navigation'});

        await expect(mainNavigation.getByRole('link', {name: 'Projects'})).toBeVisible();

        await expect(mainNavigation.locator(`a[href="${AI_HUB_PATH}"]`)).toHaveCount(0);
    });

    test('direct links to Projects and the AI Hub settings still work with the hub on', async ({
        authenticatedPage: page,
    }) => {
        await prepareAiHub(page);

        await page.goto(PROJECTS_PATH);

        await expect(page).toHaveURL((url) => url.pathname === PROJECTS_PATH);

        await expect(
            page.getByRole('navigation', {name: 'Main navigation'}).getByRole('link', {name: 'Projects'})
        ).toBeVisible();

        await page.goto('/automation/settings/ai-hub/tool-approvals');

        await expect(page.getByText('Tool Approvals', {exact: true}).first()).toBeVisible();
    });
});
